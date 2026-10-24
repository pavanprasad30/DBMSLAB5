const API_URL = "/api/records";

const form = document.getElementById("recordForm");
const patientIdInput = document.getElementById("patientId");
const recordsBody = document.getElementById("recordsBody");
const recordCount = document.getElementById("recordCount");
const message = document.getElementById("message");
const connectionStatus = document.getElementById("connectionStatus");
const actionButtons = [
  document.getElementById("additionButton"),
  document.getElementById("updationButton"),
  document.getElementById("deletionButton")
];

async function request(url, options = {}) {
  const response = await fetch(url, options);
  let payload = {};

  try {
    payload = await response.json();
  } catch {
    // The server may return an empty or non-JSON error response.
  }

  if (!response.ok) {
    throw new Error(payload.message || `Request failed (${response.status}).`);
  }
  return payload;
}

function showMessage(text, kind) {
  message.textContent = text;
  message.className = `message ${kind}`;
  message.hidden = false;
}

function setConnectionStatus(connected) {
  connectionStatus.className = `connection-status ${connected ? "connected" : "disconnected"}`;
  connectionStatus.textContent = connected
    ? "Java backend and Oracle database connected."
    : "Not connected — configure and test the Java backend and Oracle database.";
}

function showTableMessage(text) {
  recordsBody.replaceChildren();
  const row = document.createElement("tr");
  const cell = document.createElement("td");
  cell.className = "table-message";
  cell.colSpan = 6;
  cell.textContent = text;
  row.append(cell);
  recordsBody.append(row);
  recordCount.textContent = "0 records";
}

function renderRecords(records) {
  recordsBody.replaceChildren();

  if (records.length === 0) {
    showTableMessage("No patient/test records are stored in the database yet.");
    return;
  }

  for (const record of records) {
    const row = document.createElement("tr");
    const values = [
      record.patientId,
      record.patientName,
      record.age,
      record.gender,
      record.testName,
      record.testResult
    ];

    values.forEach((value, index) => {
      const cell = document.createElement("td");
      if (index === 5) cell.className = "result-cell";
      cell.textContent = value === null || value === undefined ? "—" : String(value);
      row.append(cell);
    });
    recordsBody.append(row);
  }

  recordCount.textContent = `${records.length} ${records.length === 1 ? "record" : "records"}`;
}

async function loadRecords() {
  try {
    const records = await request(API_URL);
    if (!Array.isArray(records)) {
      throw new Error("The backend returned an unexpected records response.");
    }
    renderRecords(records);
    setConnectionStatus(true);
  } catch (error) {
    showTableMessage("Records could not be loaded. Check the Java backend and Oracle setup.");
    setConnectionStatus(false);
  }
}

function readRecord() {
  return {
    patientId: document.getElementById("patientId").value.trim(),
    patientName: document.getElementById("patientName").value.trim(),
    age: Number(document.getElementById("age").value),
    gender: document.getElementById("gender").value,
    testName: document.getElementById("testName").value.trim(),
    testResult: document.getElementById("testResult").value.trim()
  };
}

function setBusy(busy) {
  actionButtons.forEach((button) => {
    button.disabled = busy;
  });
}

async function performAction(action) {
  setBusy(true);
  try {
    const result = await action();
    showMessage(result.message || "Operation completed successfully.", "success");
    await loadRecords();
  } catch (error) {
    const detail = error instanceof Error ? error.message : "Please try again.";
    showMessage(`The operation could not be completed. ${detail}`, "error");
  } finally {
    setBusy(false);
  }
}

document.getElementById("additionButton").addEventListener("click", () => {
  if (!form.reportValidity()) return;
  const record = readRecord();
  performAction(() => request(API_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(record)
  }));
});

document.getElementById("updationButton").addEventListener("click", () => {
  if (!form.reportValidity()) return;
  const record = readRecord();
  performAction(() => request(`${API_URL}/${encodeURIComponent(record.patientId)}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(record)
  }));
});

document.getElementById("deletionButton").addEventListener("click", () => {
  if (!patientIdInput.reportValidity()) return;
  const patientId = patientIdInput.value.trim();
  if (!window.confirm(`Delete the record for Patient ID ${patientId}?`)) return;

  performAction(() => request(`${API_URL}/${encodeURIComponent(patientId)}`, {
    method: "DELETE"
  }));
});

loadRecords();
