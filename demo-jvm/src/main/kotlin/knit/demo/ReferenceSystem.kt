package knit.demo

import knit.Provides
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

// Reference management system - manages branches, tags, HEAD, etc.
@Provides
class MemoryReferenceManager  {
    private val refs = ConcurrentHashMap<String, Ref>()
    private var headRef: Ref = Ref("HEAD", "")
    
     fun createBranch(name: String, commitSha: String): Boolean {
        if (!isRefNameValid(name) || !commitSha.matches(Regex("[0-9a-f]{40}"))) {
            return false
        }
        refs["refs/heads/$name"] = Ref("refs/heads/$name", commitSha)
        return true
    }
    
     fun deleteBranch(name: String): Boolean {
        val fullName = "refs/heads/$name"
        if (refs.containsKey(fullName)) {
            refs.remove(fullName)
            return true
        }
        return false
    }
    
     fun getBranch(name: String): Ref? {
        return refs["refs/heads/$name"]
    }
    
     fun listBranches(): List<Ref> {
        return refs.values.filter { it.isBranch() }
    }
    
     fun createTag(name: String, commitSha: String): Boolean {
        if (!isRefNameValid(name) || !commitSha.matches(Regex("[0-9a-f]{40}"))) {
            return false
        }
        refs["refs/tags/$name"] = Ref("refs/tags/$name", commitSha)
        return true
    }
    
     fun deleteTag(name: String): Boolean {
        val fullName = "refs/tags/$name"
        if (refs.containsKey(fullName)) {
            refs.remove(fullName)
            return true
        }
        return false
    }
    
     fun getTag(name: String): Ref? {
        return refs["refs/tags/$name"]
    }
    
     fun listTags(): List<Ref> {
        return refs.values.filter { it.isTag() }
    }
    
     fun updateHead(sha: String): Boolean {
        if (!sha.matches(Regex("[0-9a-f]{40}"))) {
            return false
        }
        headRef.sha = sha
        return true
    }
    
     fun getHead(): Ref = headRef
    
     fun getCurrentCommitSha(): String? {
        return headRef.sha.ifEmpty { null }
    }
    
     fun getAllRefs(): List<Ref> {
        return refs.values.toList() + headRef
    }
    
     fun resolveRef(refName: String): String? {
        return when {
            refName == "HEAD" -> headRef.sha
            refName.startsWith("refs/") -> refs[refName]?.sha
            refs.containsKey("refs/heads/$refName") -> refs["refs/heads/$refName"]?.sha
            refs.containsKey("refs/tags/$refName") -> refs["refs/tags/$refName"]?.sha
            else -> null
        }
    }
    
     fun isRefNameValid(name: String): Boolean {
        return name.isNotEmpty() && 
               !name.contains("..") && 
               !name.startsWith("/") && 
               !name.endsWith("/") &&
               !name.contains("//") &&
               name.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }
    }
}

// Reference history tracker
@Provides
class ReferenceHistoryTracker(
     private val refManager: MemoryReferenceManager,
     private val eventBus: EventBus
) {
    
    private val refHistory = ConcurrentHashMap<String, MutableList<RefChange>>()
    
    data class RefChange(
        val refName: String,
        val oldSha: String,
        val newSha: String,
        val timestamp: Instant,
        val operation: String
    )
    
    fun recordChange(refName: String, oldSha: String, newSha: String, operation: String) {
        val change = RefChange(refName, oldSha, newSha, Instant.now(), operation)
        refHistory.getOrPut(refName) { mutableListOf() }.add(change)
        eventBus.publish(ReferenceEvent("reference", refName, oldSha, newSha))
    }
    
    fun getRefHistory(refName: String): List<RefChange> {
        return refHistory[refName] ?: emptyList()
    }
    
    fun getAllHistory(): Map<String, List<RefChange>> {
        return refHistory.toMap()
    }
    
    fun getRecentChanges(limit: Int = 10): List<RefChange> {
        return refHistory.values.flatten()
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
}

// Reference validation service

class ReferenceValidator(
     private val refManager: MemoryReferenceManager,
     private val objectStore: MemoryObjectStore,
     private val historyTracker: ReferenceHistoryTracker
) {
    
    fun validateRefIntegrity(refName: String): Boolean {
        val sha = refManager.resolveRef(refName) ?: return false
        return objectStore.exists(sha)
    }
    
    fun validateAllRefs(): Map<String, Boolean> {
        return refManager.getAllRefs().associate { ref ->
            ref.name to validateRefIntegrity(ref.name)
        }
    }
    
    fun findBrokenRefs(): List<String> {
        return refManager.getAllRefs()
            .filter { !validateRefIntegrity(it.name) }
            .map { it.name }
    }
    
    fun findDanglingCommits(): List<String> {
        val allCommits = objectStore.getObjectsByType("commit").map { it as Commit }
        val referencedCommits = refManager.getAllRefs()
            .mapNotNull { ref -> refManager.resolveRef(ref.name) }
        
        return allCommits.filter { it.sha !in referencedCommits }.map { it.sha }
    }
    
    fun getRefReachability(refName: String): RefReachability {
        val sha = refManager.resolveRef(refName) ?: return RefReachability(refName, emptyList(), emptyList())
        
        val reachable = mutableSetOf<String>()
        val queue = mutableListOf(sha)
        
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current in reachable) continue
            reachable.add(current)
            
            val obj = objectStore.retrieve(current) ?: continue
            when (obj) {
                is Commit -> {
                    queue.add(obj.getTreeSha())
                    queue.addAll(obj.getParentShas())
                }
                is Tree -> {
                    obj.getEntries().forEach { entry ->
                        queue.add(entry.sha)
                    }
                }
                else -> {}
            }
        }
        
        val unreachable = objectStore.getAllObjects().filter { it !in reachable }
        return RefReachability(refName, reachable.toList(), unreachable)
    }
}

data class RefReachability(
    val refName: String,
    val reachableObjects: List<String>,
    val unreachableObjects: List<String>
)

// Reference events are defined in EventSystem.kt