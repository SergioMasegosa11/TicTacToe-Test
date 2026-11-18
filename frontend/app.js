// Frontend-Logik: bettet die Web-Konsole ein und zeigt Backend-/Frontend-Version an.
// Bewusst plain JavaScript ohne Framework und ohne Build-Step (siehe Auftrag).
(function () {
  "use strict";

  var config = window.TICTACTEST_CONFIG || {};

  // 1. Web-Konsole einbetten: die URL kommt aus config.js (pro Umgebung verschieden).
  var consoleFrame = document.getElementById("console");
  if (config.consoleUrl) {
    consoleFrame.src = config.consoleUrl;
  } else {
    document.getElementById("console-hint").classList.remove("hidden");
  }

  // 2. Frontend-Version: stammt aus version.js (beim Release automatisch gesetzt).
  document.getElementById("frontend-version").textContent =
    window.FRONTEND_VERSION || "unbekannt";

  // 3. Backend-Version: per HTTP vom Versions-Endpoint der Instanz abfragen.
  //    Der Endpoint liefert den Inhalt von version.txt, der beim Deployment aus dem
  //    Release-Tag geschrieben wird (deploy/bootstrap.sh).
  var backendEl = document.getElementById("backend-version");
  if (config.versionUrl) {
    fetch(config.versionUrl)
      .then(function (response) {
        if (!response.ok) {
          throw new Error("HTTP " + response.status);
        }
        return response.text();
      })
      .then(function (text) {
        backendEl.textContent = text.trim() || "unbekannt";
      })
      .catch(function (err) {
        // Haeufigste Ursachen: Instanz/Endpoint laeuft nicht, oder CORS blockiert
        // (Details in der Entwickler-Konsole des Browsers).
        console.error("Backend-Version nicht abfragbar:", err);
        backendEl.textContent = "nicht erreichbar";
      });
  } else {
    backendEl.textContent = "nicht konfiguriert";
  }
})();
