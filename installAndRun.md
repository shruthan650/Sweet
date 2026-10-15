# Install and run Sweet

Sweet is a JavaFX desktop city simulator. Its desktop client connects to the hosted Sweet API over the internet.

## Before you download

- You need an internet connection and an API access token issued for the hosted Sweet service. The token is not included in downloads. Ask the project maintainer how to obtain access; never copy a token from a public post or commit one to GitHub.
- The native app images include a Java runtime. The Windows JAR bundle requires Java 25.
- **Release note:** `v0.1.4`'s Windows and Linux native app images exit at startup because their JavaFX modules are not configured. The packaging fix is in the repository, but a corrected release still needs to be published. Until then, Windows users with Java 25 can use the JAR bundle below. Check the [latest release](https://github.com/shruthan650/Sweet/releases/latest) for a later version before using a native app image.

## Windows: native app image (no Java installation required)

After a corrected native app release is available:

1. Open [GitHub Releases](https://github.com/shruthan650/Sweet/releases/latest) and download `sweet-windows-x64.tar.gz` from **Assets**.
2. Extract the archive with File Explorer or 7-Zip. Open PowerShell in the extracted `Sweet` folder, beside `Sweet.exe`.
3. Set the hosted API URL and enter your token at the hidden prompt. Launch the app:

   ```powershell
   $env:SWEET_API_BASE_URL = "https://sweet-yqdu.onrender.com"
   $secureToken = Read-Host "Sweet API token" -AsSecureString
   $env:SWEET_API_ACCESS_TOKEN = (New-Object System.Net.NetworkCredential("", $secureToken)).Password
   .\Sweet.exe
   Remove-Item Env:SWEET_API_ACCESS_TOKEN
   ```

   The token is held only in that PowerShell session and is not saved by the app. Do not add it to a script or commit it to a repository.

## Windows: JAR bundle (Java 25 required)

1. Install a 64-bit Java 25 runtime if needed.
2. From the latest release, download `sweet-desktop-windows-x64-jar.zip` and extract it to a folder.
3. In the extracted folder, copy `env.example` to `.env`. Open `.env` in Notepad and set `SWEET_API_ACCESS_TOKEN` to the token issued to you. Keep `.env` private.
4. Open PowerShell in that folder and start the app:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run.ps1
   ```

## Linux: native app image (no Java installation required)

After a corrected native app release is available:

1. Download `sweet-linux-x64.tar.gz` from the latest release and extract it:

   ```bash
   mkdir -p "$HOME/Sweet"
   tar -xzf "$HOME/Downloads/sweet-linux-x64.tar.gz" -C "$HOME/Sweet"
   ```

2. Open a terminal in the extracted `Sweet` folder. Set the API URL, enter your token without echoing it, and launch the app:

   ```bash
   export SWEET_API_BASE_URL="https://sweet-yqdu.onrender.com"
   read -rsp "Sweet API token: " SWEET_API_ACCESS_TOKEN
   printf '\n'
   export SWEET_API_ACCESS_TOKEN
   chmod +x ./Sweet
   ./Sweet
   unset SWEET_API_ACCESS_TOKEN
   ```

## Use the simulator

When the app opens, select a city from the list. The hosted service currently has saved cities, so the starter demo is only created automatically if the city list is empty. Choose a city you are allowed to modify, then use **Start**, **Pause**, **Step**, and **Reset** to control the simulation. Avoid editing or deleting cities you do not own.

## Troubleshooting

- **Unauthorized / HTTP 401:** check that the API token was issued for the hosted service and that it was entered correctly. Do not send the token in a screenshot or issue.
- **Connection refused or timeout:** check your internet connection and try again. The hosted free service may take time to wake up after inactivity.
- **Windows native image closes immediately:** check the release notes and download a version newer than `v0.1.4` that includes the JavaFX module startup fix.
- **The JAR launcher says Java is missing:** install Java 25 and confirm `java -version` works in PowerShell.
