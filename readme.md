# BulkActions + ScheduledActions Demo (Java SDK)

This repository contains an end-to-end demo that shows how to create, scale, and delete VMs using the **Azure Java SDK**.  
It demonstrates:  

- **BulkActions API** for VM creation and scaling  
- **ScheduledActions Bulk Delete API** for VM deletion  

---

## Prerequisites

- [Java 17+](https://adoptium.net/) installed locally  
- [Maven](https://maven.apache.org/) or [Gradle](https://gradle.org/)  
- [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli) (for login)  

---

## Setup

1. Clone this repository:  
   ```bash
   git clone https://github.com/tissabre/bulkactions-go-sdk-demo.git
   cd bulkactions-java-sdk-demo
   ```

2. Authenticate with Azure CLI:
   ```bash
   az login
   ```

3. Build and run the program (Maven)
   ```bash
   mvn clean install
   mvn exec:java -Dexec.mainClass="com.example.BulkActionsDemo"
   ```

   or if you're using Gradle
   ```bash
   ./gradlew run
   ```

## Demo Recording
[BulkActions Create Delete Demo - Java SDK.mp4](./recording.mp4)