const tokenInput = document.getElementById("token");
const statusEl = document.getElementById("status");
const errorEl = document.getElementById("error");
const statsEl = document.getElementById("stats");
const usersSection = document.getElementById("usersSection");
const subsSection = document.getElementById("subsSection");
const searchInput = document.getElementById("search");
const tabUsers = document.getElementById("tabUsers");
const tabSubs = document.getElementById("tabSubs");
const saveTokenButton = document.getElementById("saveToken");
const refreshButton = document.getElementById("refreshAll");
let usersData = [];
let subsData = [];
tokenInput.value = localStorage.getItem("adminToken") ?? "";
saveTokenButton.addEventListener("click", () => {
    localStorage.setItem("adminToken", tokenInput.value.trim());
    setStatus("Токен сохранён");
});
refreshButton.addEventListener("click", () => {
    void loadAll();
});
searchInput.addEventListener("input", renderActiveTab);
tabUsers.addEventListener("click", () => switchTab("users"));
tabSubs.addEventListener("click", () => switchTab("subs"));
function switchTab(tab) {
    const usersActive = tab === "users";
    tabUsers.classList.toggle("active", usersActive);
    tabSubs.classList.toggle("active", !usersActive);
    usersSection.style.display = usersActive ? "block" : "none";
    subsSection.style.display = usersActive ? "none" : "block";
    renderActiveTab();
}
function setStatus(text) {
    statusEl.textContent = text;
}
function bytesToGb(bytes) {
    if (bytes == null)
        return "безлимит";
    return `${Math.floor(bytes / 1024 / 1024 / 1024)} GB`;
}
function dateOrUnlimited(value) {
    if (!value)
        return "без ограничений";
    return new Date(value).toLocaleString();
}
async function apiGet(path) {
    const token = tokenInput.value.trim();
    const response = await fetch(path, {
        headers: {
            "X-Admin-Token": token
        }
    });
    if (!response.ok) {
        let message = `HTTP ${response.status}`;
        try {
            const body = (await response.json());
            if (body?.message) {
                message = body.message;
            }
        }
        catch {
            // ignore json parse errors
        }
        throw new Error(message);
    }
    return (await response.json());
}
async function loadAll() {
    errorEl.textContent = "";
    setStatus("Загрузка...");
    try {
        const [overview, users, subs] = await Promise.all([
            apiGet("/api/admin/overview"),
            apiGet("/api/admin/users"),
            apiGet("/api/admin/subscriptions")
        ]);
        usersData = users;
        subsData = subs;
        renderStats(overview);
        renderActiveTab();
        setStatus(`Обновлено: ${new Date().toLocaleTimeString()}`);
    }
    catch (error) {
        const message = error instanceof Error ? error.message : "Unknown error";
        errorEl.textContent = `Ошибка: ${message}`;
        setStatus("Ошибка загрузки");
    }
}
function renderStats(overview) {
    const items = [
        ["Всего пользователей", overview.totalUsers],
        ["Всего подписок", overview.totalSubscriptions],
        ["Активных подписок", overview.activeSubscriptions],
        ["Истекших подписок", overview.expiredSubscriptions],
        ["Отозванных подписок", overview.revokedSubscriptions],
        ["Суммарный трафик", bytesToGb(overview.totalTrafficUsedBytes)]
    ];
    statsEl.innerHTML = items
        .map(([label, value]) => `
        <div class="stat">
            <div class="muted">${label}</div>
            <div class="value">${value}</div>
        </div>
    `)
        .join("");
}
function renderActiveTab() {
    const usersActive = tabUsers.classList.contains("active");
    const query = searchInput.value.trim().toLowerCase();
    if (usersActive) {
        renderUsers(query);
    }
    else {
        renderSubscriptions(query);
    }
}
function includesQuery(query, ...values) {
    if (!query) {
        return true;
    }
    return values.some((value) => String(value ?? "").toLowerCase().includes(query));
}
function renderUsers(query) {
    const rows = usersData.filter((user) => includesQuery(query, user.username, user.telegramId, user.activePlanCode, user.xrayClientUuid, user.xrayClientEmail));
    usersSection.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>User ID</th>
                    <th>Telegram ID</th>
                    <th>Username</th>
                    <th>Created</th>
                    <th>Active Plan</th>
                    <th>Active Until</th>
                    <th>Status</th>
                    <th>Xray UUID</th>
                    <th>Xray Email</th>
                    <th>Total Subs</th>
                    <th>Traffic Used</th>
                </tr>
            </thead>
            <tbody>
                ${rows
        .map((user) => `
                    <tr>
                        <td>${user.userId}</td>
                        <td>${user.telegramId}</td>
                        <td>${user.username ?? ""}</td>
                        <td>${dateOrUnlimited(user.createdAt)}</td>
                        <td>${user.activePlanCode ?? "-"}</td>
                        <td>${dateOrUnlimited(user.activeEndsAt)}</td>
                        <td>${user.activeStatus ?? "-"}</td>
                        <td>${user.xrayClientUuid ?? "-"}</td>
                        <td>${user.xrayClientEmail ?? "-"}</td>
                        <td>${user.totalSubscriptions}</td>
                        <td>${bytesToGb(user.totalTrafficUsedBytes)}</td>
                    </tr>
                `)
        .join("")}
            </tbody>
        </table>
    `;
}
function renderSubscriptions(query) {
    const rows = subsData.filter((subscription) => includesQuery(query, subscription.username, subscription.telegramId, subscription.planCode, subscription.status, subscription.xrayClientUuid, subscription.xrayClientEmail));
    subsSection.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>Sub ID</th>
                    <th>User ID</th>
                    <th>Telegram ID</th>
                    <th>Username</th>
                    <th>Plan</th>
                    <th>Status</th>
                    <th>Starts</th>
                    <th>Ends</th>
                    <th>Traffic Limit</th>
                    <th>Traffic Used</th>
                    <th>Devices</th>
                    <th>Xray UUID</th>
                    <th>Xray Email</th>
                </tr>
            </thead>
            <tbody>
                ${rows
        .map((subscription) => `
                    <tr>
                        <td>${subscription.subscriptionId}</td>
                        <td>${subscription.userId}</td>
                        <td>${subscription.telegramId}</td>
                        <td>${subscription.username ?? ""}</td>
                        <td>${subscription.planCode}</td>
                        <td>${subscription.status}</td>
                        <td>${dateOrUnlimited(subscription.startsAt)}</td>
                        <td>${dateOrUnlimited(subscription.endsAt)}</td>
                        <td>${bytesToGb(subscription.trafficLimitBytes)}</td>
                        <td>${bytesToGb(subscription.trafficUsedBytes)}</td>
                        <td>${subscription.deviceLimit == null ? "безлимит" : subscription.deviceLimit}</td>
                        <td>${subscription.xrayClientUuid}</td>
                        <td>${subscription.xrayClientEmail}</td>
                    </tr>
                `)
        .join("")}
            </tbody>
        </table>
    `;
}
void loadAll();
export {};
