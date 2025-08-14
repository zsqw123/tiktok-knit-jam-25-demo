package knit.demo

import knit.Provides
import java.util.Scanner

// CLI interactive system
@Provides
class SampleCli(
    private val commandRegistry: CommandRegistry,
    private val eventBus: EventBus,
    private val auditLogger: AuditLogger,
    private val performanceMonitor: PerformanceMonitor,
    private val fileSystem: MemoryFileSystem,
    private val refManager: MemoryReferenceManager,
    private val objectStore: MemoryObjectStore,
    private val objectGraphAnalyzer: ObjectGraphAnalyzer
) {
    private val scanner = Scanner(System.`in`)
    private var running = true

    fun start() {
        while (running) {
            val currentDir = fileSystem.getCurrentDirectory()
            val branch = refManager.listBranches()
                .find { it.sha == refManager.getCurrentCommitSha() }
                ?.getShortName() ?: "HEAD"

            print("git:$branch:${currentDir.path} > ")
            val input = scanner.nextLine().trim()

            if (input.isEmpty()) continue

            val parts = input.split(" ")
            val command = parts[0]
            val args = parts.drop(1)

            when (command) {
                "exit", "quit" -> {
                    running = false
                }

                "help" -> println(commandRegistry.getHelp())
                else -> executeCommand(command, args)
            }
        }
    }

    private fun executeCommand(command: String, args: List<String>) {
        try {
            val result = commandRegistry.executeCommand(command, args)
            when (result) {
                is CommandResult.Success -> println(result.message)
                is CommandResult.Error -> println(result.message)
                is CommandResult.Info -> println(result.data)
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }
}
