import fs from "node:fs";
import path from "node:path";

const sourceFile = path.resolve("target/admin-ts/app.js");
const targetFile = path.resolve("src/main/resources/static/admin/app.js");
const sourceDir = path.dirname(sourceFile);

function copyIfExists() {
    if (!fs.existsSync(sourceFile)) {
        return;
    }
    fs.mkdirSync(path.dirname(targetFile), { recursive: true });
    fs.copyFileSync(sourceFile, targetFile);
    console.log(`[admin-copy-watch] copied ${sourceFile} -> ${targetFile}`);
}

copyIfExists();

if (!fs.existsSync(sourceDir)) {
    fs.mkdirSync(sourceDir, { recursive: true });
}

fs.watch(sourceDir, { persistent: true }, (eventType, filename) => {
    if (!filename) {
        return;
    }
    if (filename.toString() === "app.js" && (eventType === "change" || eventType === "rename")) {
        try {
            copyIfExists();
        } catch (error) {
            console.error("[admin-copy-watch] copy failed", error);
        }
    }
});

console.log("[admin-copy-watch] watching for target/admin-ts/app.js changes...");
