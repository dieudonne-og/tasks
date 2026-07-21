# Running TaskMS on Windows

The project is already on this PC. This guide only shows how to **start** it.

---

## One-time setup — Java 17

Only needed if Java is not installed yet.

1. Download: https://adoptium.net/temurin/releases/?version=17 (pick **Windows x64 .msi**)
2. Run the installer, click **Next** to the end.
3. If offered, turn on **"Set JAVA_HOME"** and **"Add to PATH"**.
4. Check it works: press **Windows key**, type `cmd`, **Enter**, then:
   ```cmd
   java -version
   ```
   You should see `openjdk version "17..."`.

---

## Start the app

1. Open the **tasks** project folder in File Explorer.
2. Click the address bar at the top, type `cmd`, press **Enter** — a black window
   opens already inside the folder.
3. Type this and press **Enter**:
   ```cmd
   mvnw.cmd spring-boot:run
   ```
4. Wait until you see:
   ```
   Started TaskmsApplication in 23 seconds
   ```
   Leave this black window **open** — closing it stops the app.

---

## Open it

1. Open Chrome or Edge.
2. Go to **http://localhost:8080**
3. Log in:

   | Role | Email | Password |
   |------|-------|----------|
   | Admin | `admin@uok.ac.rw` | `admin123` |
   | HR Manager | `manager@uok.ac.rw` | `manager123` |
   | HR Officer | `alice@uok.ac.rw` | `officer123` |

You land on the **Dashboard**.

---

## Stop the app

Click the black window, press **Ctrl + C**, type `Y`, **Enter**.

---

## If something goes wrong

| Problem | Fix |
|---------|-----|
| `java is not recognized` | Install Java 17 (above), restart the PC. |
| `mvnw.cmd is not recognized` | The `cmd` window is not inside the **tasks** folder. Redo the address-bar step. |
| Port `8080` in use | Run `mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8090"` and open http://localhost:8090 |
| Page won't load | Black window must still show *Started TaskmsApplication*. |
| Data resets after restart | Normal — default database is temporary. |
