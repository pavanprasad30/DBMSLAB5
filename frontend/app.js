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
  const result = await response.json();
  if (!response.ok) throw new Error(result.message || `Request failed (${response.status}).`);
  return result;
}

function showMessage(text, type) {
  message.textContent = text;
  message.className = `message ${type}`;
  message.hidden = false;
}

function showTableMessage(text) {
  recordsBody.replaceChildren();
  const row = document.createElement("tr");
  const cell = document.createElement("td");
  cell.className = "table-message";
  cell.colSpan = 5;
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
    [record.patientId, record.name, record.age, record.testName, record.testResult].forEach((value) => {
      const cell = document.createElement("td");
      cell.textContent = value === null || value === undefined ? "—" : String(value);
      row.append(cell);
    });
    recordsBody.append(row);
  }
  recordCount.textContent = `${records.length} ${records.length === 1 ? "record" : "records"}`;
}

async function loadRecords() {
  try {
    renderRecords(await request(API_URL));
    connectionStatus.className = "connection-status connected";
    connectionStatus.textContent = "Java backend and Oracle database connected.";
  } catch {
    showTableMessage("Records could not be loaded. Check the Java backend and Oracle setup.");
    connectionStatus.className = "connection-status disconnected";
    connectionStatus.textContent = "Not connected — configure and test Java and Oracle.";
  }
}

function readRecord() {
  return {
    patientId: patientIdInput.value.trim(),
    name: document.getElementById("name").value.trim(),
    age: Number(document.getElementById("age").value),
    testName: document.getElementById("testName").value.trim(),
    testResult: document.getElementById("testResult").value.trim()
  };
}

async function performAction(action) {
  actionButtons.forEach((button) => { button.disabled = true; });
  try {
    const result = await action();
    showMessage(result.message, "success");
    await loadRecords();
  } catch (error) {
    showMessage(`Operation failed: ${error.message}`, "error");
  } finally {
    actionButtons.forEach((button) => { button.disabled = false; });
  }
}

document.getElementById("additionButton").addEventListener("click", () => {
  if (!form.reportValidity()) return;
  performAction(() => request(API_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(readRecord())
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
  performAction(() => request(`${API_URL}/${encodeURIComponent(patientId)}`, { method: "DELETE" }));
});

loadRecords();
