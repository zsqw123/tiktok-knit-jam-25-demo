package knit.demo

import knit.Component
import knit.Provides

@Component
@Provides
class StoreComponent(
    @Provides
    val fileSystem: MemoryFileSystem,
    @Provides
    val refManager: MemoryReferenceManager,
    @Provides
    val objectStore: MemoryObjectStore,
)

