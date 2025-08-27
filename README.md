# Knit TechJam 25 Demo Project

## How to run

### 1. from shell

1. Environment requirements
    - JDK 11 (it might work with higher version, but not tested), make sure `JAVA_HOME` is set

2. Build the demo with Knit (ensure run this command in the root directory of this project)
   ```bash
   ./gradlew :demo-jvm:shadowJarWithKnit
   ```

3. Run the demo
   ```bash
   java -jar demo-jvm/build/libs/demo-jvm-allWithKnit.jar
   ```
   then you can see the output like this, trying input some git commands!
   ```
   git:HEAD:/ > 
   ```

### 2. from IDEA

1. Open this gradle project in IDEA
   - then open `Settings | Build, Execution, Deployment | Build Tools | Gradle`, make sure `Gradle JVM` is set to JDK
     11 (download JDK here if needed)
   - then click `Apply` and `OK`
2. Run the demo
   - I already created a run configuration for you, just select `RunDemo` and run it.
   - then you can see the output like this, trying input some git commands!
   ```
   git:HEAD:/ > 
   ```

## Check dependency graph

After each build, you can check the dependency graph in `demo-jvm/build/knit.json`, it should be like this:

```json5
{
  "knit/demo/ObjectIndexService": {
    "parent": [
      // ...
    ],
    "providers": [
      // ...
    ]
  },
  "knit/demo/AuditLogger": {
    // ...
  },
  "knit/demo/MemoryReferenceManager": {
    // ...
  },
  // ...
}
```
