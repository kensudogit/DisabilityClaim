#!/usr/bin/env node
/**
 * Collect backend Surefire XML + frontend Vitest JSON into
 * frontend/public/test-reports/manifest.json for the /test-reports page.
 */
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const publicRoot = path.join(root, "frontend", "public", "test-reports");
const surefireXmlDir = path.join(root, "backend", "build", "test-results", "test");
const vitestJson = path.join(publicRoot, "frontend", "vitest", "results.json");

function parseSurefireSummary() {
  if (!fs.existsSync(surefireXmlDir)) return undefined;
  const files = fs.readdirSync(surefireXmlDir).filter((f) => f.endsWith(".xml"));
  let tests = 0;
  let failures = 0;
  let errors = 0;
  let skipped = 0;
  for (const file of files) {
    const xml = fs.readFileSync(path.join(surefireXmlDir, file), "utf8");
    const match = xml.match(
      /<testsuite[^>]*tests="(\d+)"[^>]*skipped="(\d+)"[^>]*failures="(\d+)"[^>]*errors="(\d+)"/,
    );
    const alt = xml.match(
      /<testsuite[^>]*tests="(\d+)"[^>]*failures="(\d+)"[^>]*errors="(\d+)"[^>]*skipped="(\d+)"/,
    );
    if (match) {
      tests += Number(match[1]);
      skipped += Number(match[2]);
      failures += Number(match[3]);
      errors += Number(match[4]);
    } else if (alt) {
      tests += Number(alt[1]);
      failures += Number(alt[2]);
      errors += Number(alt[3]);
      skipped += Number(alt[4]);
    }
  }
  return { tests, failures, errors, skipped };
}

function parseVitestSummary() {
  if (!fs.existsSync(vitestJson)) return undefined;
  const data = JSON.parse(fs.readFileSync(vitestJson, "utf8"));
  return {
    numTotalTests: data.numTotalTests,
    numPassedTests: data.numPassedTests,
    numFailedTests: data.numFailedTests,
    numPendingTests: data.numPendingTests,
  };
}

fs.mkdirSync(publicRoot, { recursive: true });

const manifest = {
  generatedAt: new Date().toISOString(),
  backend: {
    surefireIndex: "/test-reports/backend/surefire/index.html",
    jacocoIndex: "/test-reports/backend/jacoco/index.html",
    summary: parseSurefireSummary(),
  },
  frontend: {
    vitestIndex: "/test-reports/frontend/vitest/index.html",
    coverageIndex: "/test-reports/frontend/coverage/index.html",
    summary: parseVitestSummary(),
  },
};

fs.writeFileSync(path.join(publicRoot, "manifest.json"), JSON.stringify(manifest, null, 2));
console.log("Wrote", path.join(publicRoot, "manifest.json"));
console.log(JSON.stringify(manifest, null, 2));
