const STORAGE_KEY = "weight_records_v1";
const UNIT_KEY = "weight_display_unit_v1";
const UNIT = { KG: "kg", JIN: "jin" };

const state = {
  homeUnit: localStorage.getItem(UNIT_KEY) || UNIT.KG,
  addUnit: localStorage.getItem(UNIT_KEY) || UNIT.KG,
  rangeMode: 7,
  records: loadRecords(),
  chart: null,
};

const els = {
  toggleUnitBtn: document.getElementById("toggleUnitBtn"),
  menuBtn: document.getElementById("menuBtn"),
  startDate: document.getElementById("startDate"),
  endDate: document.getElementById("endDate"),
  applyCustomBtn: document.getElementById("applyCustomBtn"),
  tableBody: document.getElementById("tableBody"),
  tableTitle: document.getElementById("tableTitle"),
  emptyState: document.getElementById("emptyState"),
  addBtn: document.getElementById("addBtn"),
  addDialog: document.getElementById("addDialog"),
  recordDate: document.getElementById("recordDate"),
  recordWeight: document.getElementById("recordWeight"),
  weightUnitLabel: document.getElementById("weightUnitLabel"),
  addUnitToggleBtn: document.getElementById("addUnitToggleBtn"),
  addForm: document.getElementById("addForm"),
  menuDialog: document.getElementById("menuDialog"),
  downloadTemplateBtn: document.getElementById("downloadTemplateBtn"),
  exportBtn: document.getElementById("exportBtn"),
  importInput: document.getElementById("importInput"),
};

init();

function init() {
  registerServiceWorker();
  initDateInputs();
  bindEvents();
  render();
}

function registerServiceWorker() {
  if (!("serviceWorker" in navigator)) return;
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("./sw.js").catch(() => {});
  });
}

function bindEvents() {
  document.querySelectorAll("[data-range]").forEach((btn) => {
    btn.addEventListener("click", () => {
      state.rangeMode = Number(btn.dataset.range);
      document.querySelectorAll("[data-range]").forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      render();
    });
  });

  els.toggleUnitBtn.addEventListener("click", () => {
    const next = state.homeUnit === UNIT.KG ? UNIT.JIN : UNIT.KG;
    setHomeUnit(next);
  });

  els.applyCustomBtn.addEventListener("click", () => {
    if (!els.startDate.value || !els.endDate.value) return alert("请先选择开始和结束日期");
    if (els.startDate.value > els.endDate.value) return alert("开始日期不能晚于结束日期");
    state.rangeMode = "custom";
    document.querySelectorAll("[data-range]").forEach((b) => b.classList.remove("active"));
    render();
  });

  els.addBtn.addEventListener("click", openAddDialog);
  els.menuBtn.addEventListener("click", () => els.menuDialog.showModal());
  els.addUnitToggleBtn.addEventListener("click", () => {
    const next = state.addUnit === UNIT.KG ? UNIT.JIN : UNIT.KG;
    setAddUnit(next, { convertDialogValue: true });
  });

  els.addForm.addEventListener("submit", (e) => {
    e.preventDefault();
    saveRecord();
  });

  document.querySelectorAll(".quick-row button").forEach((btn) => {
    btn.addEventListener("click", () => {
      const delta = Number(btn.dataset.delta);
      const current = Number(els.recordWeight.value || 0);
      const next = round2(current + delta);
      els.recordWeight.value = next.toFixed(2);
    });
  });

  els.downloadTemplateBtn.addEventListener("click", downloadTemplate);
  els.exportBtn.addEventListener("click", exportExcel);
  els.importInput.addEventListener("change", importExcel);
}

function initDateInputs() {
  const t = todayStr();
  els.startDate.max = t;
  els.endDate.max = t;
  els.recordDate.max = t;
  els.endDate.value = t;
  els.startDate.value = dayOffsetStr(-6);
}

function openAddDialog() {
  // 每次打开添加页，默认单位与首页单位一致
  state.addUnit = state.homeUnit;
  const t = todayStr();
  const latest = state.records[state.records.length - 1];
  const defaultKg = latest ? latest.weightKg : 60.0; // 120斤
  const defaultDisplay = state.addUnit === UNIT.KG ? defaultKg : kgToJin(defaultKg);
  els.recordDate.value = t;
  els.recordDate.max = t;
  els.recordWeight.value = defaultDisplay.toFixed(2);
  updateAddDialogUnitUI();
  els.addDialog.showModal();
}

function saveRecord() {
  const d = els.recordDate.value;
  if (!d) return alert("请选择日期");
  if (d > todayStr()) return alert("不可添加未来日期");
  const input = Number(els.recordWeight.value);
  if (!Number.isFinite(input) || input <= 0) return alert("请输入有效体重");
  const weightKg = round2(state.addUnit === UNIT.KG ? input : jinToKg(input));

  const idx = state.records.findIndex((r) => r.date === d);
  if (idx >= 0) {
    const ok = confirm("该日期已有记录，是否覆盖？");
    if (!ok) return;
    state.records[idx] = { date: d, weightKg };
  } else {
    state.records.push({ date: d, weightKg });
  }
  state.records.sort((a, b) => a.date.localeCompare(b.date));
  persistRecords();
  els.addDialog.close();
  render();
}

function render() {
  const unitText = state.homeUnit === UNIT.KG ? "公斤" : "斤";
  els.toggleUnitBtn.textContent = `单位：${unitText}`;
  els.tableTitle.textContent = `记录明细（${unitText}）`;
  updateAddDialogUnitUI();
  const filtered = filteredRecords();
  renderTable(filtered);
  renderChart(filtered);
}

function setHomeUnit(nextUnit) {
  if (nextUnit !== UNIT.KG && nextUnit !== UNIT.JIN) return;
  if (nextUnit === state.homeUnit) return;
  state.homeUnit = nextUnit;
  localStorage.setItem(UNIT_KEY, state.homeUnit);
  render();
}

function setAddUnit(nextUnit, { convertDialogValue = false } = {}) {
  if (nextUnit !== UNIT.KG && nextUnit !== UNIT.JIN) return;
  if (nextUnit === state.addUnit) return;
  if (convertDialogValue) {
    const currentValue = Number(els.recordWeight.value);
    if (Number.isFinite(currentValue) && currentValue > 0) {
      const nextDisplay = nextUnit === UNIT.KG ? jinToKg(currentValue) : kgToJin(currentValue);
      els.recordWeight.value = nextDisplay.toFixed(2);
    }
  }
  state.addUnit = nextUnit;
  updateAddDialogUnitUI();
}

function updateAddDialogUnitUI() {
  const unitText = state.addUnit === UNIT.KG ? "公斤" : "斤";
  els.weightUnitLabel.textContent = unitText;
  els.addUnitToggleBtn.textContent = state.addUnit === UNIT.KG ? "切换到斤" : "切换到公斤";
}

function filteredRecords() {
  const records = [...state.records];
  if (state.rangeMode === 7 || state.rangeMode === 30) {
    const start = dayOffsetStr(-(state.rangeMode - 1));
    const end = todayStr();
    return records.filter((r) => r.date >= start && r.date <= end);
  }
  const start = els.startDate.value;
  const end = els.endDate.value;
  return records.filter((r) => r.date >= start && r.date <= end);
}

function renderTable(records) {
  els.tableBody.innerHTML = "";
  if (!records.length) {
    els.emptyState.style.display = "block";
    return;
  }
  els.emptyState.style.display = "none";
  [...records].reverse().forEach((r) => {
    const v = state.homeUnit === UNIT.KG ? r.weightKg : kgToJin(r.weightKg);
    const tr = document.createElement("tr");
    tr.innerHTML = `<td>${r.date}</td><td>${v.toFixed(2)}</td>`;
    els.tableBody.appendChild(tr);
  });
}

function renderChart(records) {
  const labels = records.map((r) => r.date.slice(5));
  const data = records.map((r) => (state.homeUnit === UNIT.KG ? r.weightKg : kgToJin(r.weightKg)));
  const ctx = document.getElementById("weightChart");

  if (state.chart) state.chart.destroy();
  state.chart = new Chart(ctx, {
    type: "line",
    data: {
      labels,
      datasets: [{
        label: `体重(${state.homeUnit === UNIT.KG ? "公斤" : "斤"})`,
        data,
        borderColor: "#2563eb",
        backgroundColor: "rgba(37,99,235,.15)",
        borderWidth: 3,
        fill: true,
        tension: 0.35,
        pointRadius: 3.5,
      }],
    },
    options: {
      responsive: true,
      plugins: { legend: { display: false } },
      scales: { y: { ticks: { precision: 2 } } },
    },
  });
}

function exportExcel() {
  const rows = filteredRecords().map((r) => ({
    日期: r.date,
    "体重(kg)": round2(r.weightKg),
    "体重(斤)": round2(kgToJin(r.weightKg)),
  }));
  const wb = XLSX.utils.book_new();
  const ws = XLSX.utils.json_to_sheet(rows.length ? rows : [{ 日期: "", "体重(kg)": "", "体重(斤)": "" }]);
  XLSX.utils.book_append_sheet(wb, ws, "体重记录");
  const name = `weight_records_${localDateTimeCompact()}.xlsx`;
  XLSX.writeFile(wb, name);
}

function downloadTemplate() {
  const wb = XLSX.utils.book_new();
  const ws = XLSX.utils.json_to_sheet([{ date: "2026-04-26", weight_kg: 60.0 }]);
  XLSX.utils.book_append_sheet(wb, ws, "模板");
  XLSX.writeFile(wb, "weight_import_template.xlsx");
}

function importExcel(e) {
  const file = e.target.files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = () => {
    try {
      const wb = XLSX.read(reader.result, { type: "array" });
      const ws = wb.Sheets[wb.SheetNames[0]];
      const rows = XLSX.utils.sheet_to_json(ws, { defval: "" });
      let success = 0;
      let failed = 0;
      rows.forEach((row) => {
        const date = String(row.date || row.日期 || "").trim();
        let kg = Number(row.weight_kg || row["体重(kg)"] || "");
        const jin = Number(row["体重(斤)"] || "");
        if (!Number.isFinite(kg) && Number.isFinite(jin)) kg = jinToKg(jin);
        if (!isDateValid(date) || date > todayStr() || !Number.isFinite(kg) || kg <= 0) {
          failed += 1;
          return;
        }
        const idx = state.records.findIndex((r) => r.date === date);
        const record = { date, weightKg: round2(kg) };
        if (idx >= 0) state.records[idx] = record;
        else state.records.push(record);
        success += 1;
      });
      state.records.sort((a, b) => a.date.localeCompare(b.date));
      persistRecords();
      render();
      alert(`导入完成：成功 ${success} 条，失败 ${failed} 条`);
    } catch (err) {
      alert(`导入失败：${err.message}`);
    } finally {
      els.importInput.value = "";
    }
  };
  reader.readAsArrayBuffer(file);
}

function loadRecords() {
  try {
    const raw = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
    return Array.isArray(raw) ? raw.filter((r) => r.date && Number.isFinite(r.weightKg)) : [];
  } catch {
    return [];
  }
}
function persistRecords() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state.records));
}
function round2(n) { return Math.round(n * 100) / 100; }
function kgToJin(kg) { return round2(kg * 2); }
function jinToKg(jin) { return round2(jin / 2); }
/** 本地日历日期 YYYY-MM-DD（避免 toISOString 用 UTC 导致国内凌晨错成“昨天”） */
function formatLocalYmd(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function todayStr() {
  return formatLocalYmd(new Date());
}

function dayOffsetStr(offset) {
  const d = new Date();
  d.setDate(d.getDate() + offset);
  return formatLocalYmd(d);
}

/** 本地时间文件名用 yyyyMMdd_HHmmss */
function localDateTimeCompact() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}_${p(d.getHours())}${p(d.getMinutes())}${p(d.getSeconds())}`;
}
function isDateValid(date) {
  return /^\d{4}-\d{2}-\d{2}$/.test(date) && !Number.isNaN(new Date(date).getTime());
}
