import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.0/firebase-app.js";
import {
  getAuth,
  GoogleAuthProvider,
  onAuthStateChanged,
  signInWithPopup,
  signOut
} from "https://www.gstatic.com/firebasejs/11.6.0/firebase-auth.js";
import {
  addDoc,
  collection,
  doc,
  getDocs,
  getFirestore,
  limit,
  orderBy,
  query,
  serverTimestamp,
  updateDoc
} from "https://www.gstatic.com/firebasejs/11.6.0/firebase-firestore.js";

/**
 * Configure your Firebase Web App credentials from Firebase Console -> Project Settings -> Your Apps -> Web App.
 * Keep this dashboard hosted privately (do NOT bundle inside the Android APK).
 */
const FIREBASE_WEB_CONFIG = window.SMILE_FIREBASE_CONFIG || {
  apiKey: "",
  authDomain: "gen-lang-client-0029743851.firebaseapp.com",
  projectId: "gen-lang-client-0029743851",
  appId: ""
};

const FIRESTORE_DATABASE_ID = "ai-studio-android-smile-33715eb6-7eef-4799-8f6b-21ffedb9e9f0";
const AUTHORIZED_ADMIN_EMAIL = "ramanrastogi2000@gmail.com";

const el = (id) => document.getElementById(id);

if (!FIREBASE_WEB_CONFIG.apiKey || !FIREBASE_WEB_CONFIG.appId) {
  el("configWarning").classList.remove("hidden");
} else {
  const app = initializeApp(FIREBASE_WEB_CONFIG);
  const auth = getAuth(app);
  const db = getFirestore(app, FIRESTORE_DATABASE_ID);

  let cachedUsers = [];

  el("signInBtn").addEventListener("click", async () => {
    try {
      el("accessError").classList.add("hidden");
      await signInWithPopup(auth, new GoogleAuthProvider());
    } catch (err) {
      showError(err.message);
    }
  });

  el("signOutBtn").addEventListener("click", () => signOut(auth));
  el("refreshBtn").addEventListener("click", () => loadDashboardData(db));
  el("userSearchInput").addEventListener("input", () => renderUsersTable());

  el("announcementForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const title = el("annTitle").value.trim();
    const body = el("annBody").value.trim();
    if (!title || !body) return;
    await addDoc(collection(db, "announcements"), {
      title,
      body,
      published: true,
      createdBy: auth.currentUser.uid,
      createdAt: serverTimestamp()
    });
    await writeAudit(db, auth.currentUser.uid, "PUBLISH_ANNOUNCEMENT", "announcements", title);
    el("annTitle").value = "";
    el("annBody").value = "";
    await loadDashboardData(db);
  });

  onAuthStateChanged(auth, async (user) => {
    if (!user) {
      el("adminUserLabel").textContent = "Not signed in";
      el("signInBtn").classList.remove("hidden");
      el("signOutBtn").classList.add("hidden");
      el("loginGate").classList.remove("hidden");
      el("dashboardContent").classList.add("hidden");
      return;
    }

    el("adminUserLabel").textContent = user.email || user.uid;
    el("signInBtn").classList.add("hidden");
    el("signOutBtn").classList.remove("hidden");

    const isOwner = user.email && user.email.toLowerCase() === AUTHORIZED_ADMIN_EMAIL.toLowerCase() && user.emailVerified;
    if (!isOwner) {
      showError("Access denied: This Google account is not an authorized SMILE Owner/Admin.");
      el("loginGate").classList.remove("hidden");
      el("dashboardContent").classList.add("hidden");
      return;
    }

    el("loginGate").classList.add("hidden");
    el("dashboardContent").classList.remove("hidden");
    await loadDashboardData(db);
  });

  async function loadDashboardData(database) {
    try {
      const [usersSnap, chatsSnap, commsSnap, reportsSnap, annSnap, auditSnap] = await Promise.all([
        getDocs(query(collection(database, "userProfiles"), limit(200))),
        getDocs(query(collection(database, "chats"), limit(200))),
        getDocs(query(collection(database, "communities"), limit(200))),
        getDocs(query(collection(database, "reports"), orderBy("createdAt", "desc"), limit(100))),
        getDocs(query(collection(database, "announcements"), orderBy("createdAt", "desc"), limit(20))),
        getDocs(query(collection(database, "auditLogs"), orderBy("createdAt", "desc"), limit(50)))
      ]);

      cachedUsers = usersSnap.docs.map((d) => ({ id: d.id, ...d.data() }));
      const reports = reportsSnap.docs.map((d) => ({ id: d.id, ...d.data() }));
      const announcements = annSnap.docs.map((d) => ({ id: d.id, ...d.data() }));
      const audits = auditSnap.docs.map((d) => ({ id: d.id, ...d.data() }));

      el("metricUsers").textContent = String(usersSnap.size);
      el("metricChats").textContent = String(chatsSnap.size);
      el("metricCommunities").textContent = String(commsSnap.size);
      el("metricReports").textContent = String(reports.filter((r) => r.reportStatus === "OPEN").length);

      renderUsersTable(database, auth.currentUser?.uid);
      renderReportsTable(database, reports, auth.currentUser?.uid);
      renderSimpleList("announcementsList", announcements.map((a) => `${a.title}: ${a.body}`));
      renderSimpleList("auditLogsList", audits.map((a) => `[${a.action}] ${a.targetPath} — ${a.details || ""}`));
    } catch (err) {
      showError("Firestore permission check failed: " + err.message);
    }
  }

  function renderUsersTable(database = db, adminUid = auth.currentUser?.uid) {
    const q = el("userSearchInput").value.trim().toLowerCase();
    const tbody = el("usersTableBody");
    tbody.innerHTML = "";
    cachedUsers
      .filter((u) => !q || (u.displayName || "").toLowerCase().includes(q) || (u.smileId || "").toLowerCase().includes(q))
      .forEach((u) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td>${escapeHtml(u.displayName || "—")}</td>
          <td>@${escapeHtml(u.smileId || "—")}</td>
          <td>${u.discoverable ? "Discoverable" : "Hidden"}</td>
          <td>${u.online ? "Online" : "Offline"}</td>
          <td></td>
        `;
        const btn = document.createElement("button");
        btn.className = "btn btn-outline btn-small";
        btn.textContent = u.discoverable ? "Hide Profile" : "Restore Visibility";
        btn.addEventListener("click", async () => {
          await updateDoc(doc(database, "userProfiles", u.id), {
            discoverable: !u.discoverable,
            updatedAt: serverTimestamp()
          });
          await writeAudit(database, adminUid, "TOGGLE_USER_VISIBILITY", `userProfiles/${u.id}`, `discoverable=${!u.discoverable}`);
          await loadDashboardData(database);
        });
        tr.lastElementChild.appendChild(btn);
        tbody.appendChild(tr);
      });
  }

  function renderReportsTable(database, reports, adminUid) {
    const tbody = el("reportsTableBody");
    tbody.innerHTML = "";
    reports.forEach((r) => {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${escapeHtml(r.targetUid || "")}</td>
        <td>${escapeHtml(r.targetType || "")}</td>
        <td>${escapeHtml(r.reason || "")}</td>
        <td>${escapeHtml(r.details || "")}</td>
        <td>${escapeHtml(r.reportStatus || "OPEN")}</td>
        <td></td>
      `;
      if (r.reportStatus !== "RESOLVED") {
        const btn = document.createElement("button");
        btn.className = "btn btn-primary btn-small";
        btn.textContent = "Resolve";
        btn.addEventListener("click", async () => {
          await updateDoc(doc(database, "reports", r.id), {
            reportStatus: "RESOLVED",
            updatedAt: serverTimestamp()
          });
          await writeAudit(database, adminUid, "RESOLVE_REPORT", `reports/${r.id}`, r.reason || "");
          await loadDashboardData(database);
        });
        tr.lastElementChild.appendChild(btn);
      }
      tbody.appendChild(tr);
    });
  }
}

async function writeAudit(db, actorUid, action, targetPath, details) {
  await addDoc(collection(db, "auditLogs"), {
    actorUid,
    action,
    targetPath,
    details,
    createdAt: serverTimestamp()
  });
}

function renderSimpleList(elementId, items) {
  const list = document.getElementById(elementId);
  list.innerHTML = "";
  if (items.length === 0) {
    const li = document.createElement("li");
    li.textContent = "No entries yet.";
    list.appendChild(li);
    return;
  }
  items.forEach((txt) => {
    const li = document.createElement("li");
    li.textContent = txt;
    list.appendChild(li);
  });
}

function showError(msg) {
  const box = document.getElementById("accessError");
  box.textContent = msg;
  box.classList.remove("hidden");
}

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (ch) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "\"": "&quot;",
    "'": "&#39;"
  }[ch]));
}
