const STORAGE_KEY = "weight_records_v1";
const UNIT_KEY = "weight_display_unit_v1";
const UNIT = { KG: "kg", JIN: "jin" };

const state = {
  // 全局默认单位：斤；用户切换后按本地存储恢复
  homeUnit: localStorage.getItem(UNIT_KEY) || UNIT.JIN,
  addUnit: localStorage.getItem(UNIT_KEY) || UNIT.JIN,
  currentMonth: firstDayOfMonth(new Date()),
  records: loadRecords(),
};

const els = {
  toggleUnitBtn: document.getElementById("toggleUnitBtn"),
  menuBtn: document.getElementById("menuBtn"),
  monthTitle: document.getElementById("monthTitle"),
  prevMonthBtn: document.getElementById("prevMonthBtn"),
  nextMonthBtn: document.getElementById("nextMonthBtn"),
  calendarGrid: document.getElementById("calendarGrid"),
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
  els.toggleUnitBtn.addEventListener("click", () => {
    const next = state.homeUnit === UNIT.KG ? UNIT.JIN : UNIT.KG;
    setHomeUnit(next);
  });

  els.prevMonthBtn.addEventListener("click", () => {
    state.currentMonth = addMonth(state.currentMonth, -1);
    renderCalendar();
  });

  els.nextMonthBtn.addEventListener("click", () => {
    state.currentMonth = addMonth(state.currentMonth, 1);
    renderCalendar();
  });

  els.addBtn.addEventListener("click", () => openAddDialog(todayStr()));
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
  els.recordDate.max = todayStr();
}

function openAddDialog(dateStr) {
  // 每次打开添加页，默认单位与首页单位一致
  state.addUnit = state.homeUnit;
  const existing = state.records.find((r) => r.date === dateStr);
  const latest = state.records[state.records.length - 1];
  // 优先回填该日期已有体重，用于编辑；否则使用最近一次，首次为 120斤(60kg)
  const defaultKg = existing?.weightKg ?? latest?.weightKg ?? 60.0;
  const defaultDisplay = state.addUnit === UNIT.KG ? defaultKg : kgToJin(defaultKg);
  const picked = dateStr && dateStr <= todayStr() ? dateStr : todayStr();
  els.recordDate.value = picked;
  els.recordDate.max = todayStr();
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
  updateAddDialogUnitUI();
  renderCalendar();
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

function renderCalendar() {
  const y = state.currentMonth.getFullYear();
  const m = state.currentMonth.getMonth();
  els.monthTitle.textContent = `${y}年${String(m + 1).padStart(2, "0")}月`;

  const map = new Map(state.records.map((r) => [r.date, r.weightKg]));
  els.calendarGrid.innerHTML = "";

  const monthStart = new Date(y, m, 1);
  const monthEnd = new Date(y, m + 1, 0);
  const daysInMonth = monthEnd.getDate();
  const firstWeekday = (monthStart.getDay() + 6) % 7;

  for (let i = 0; i < firstWeekday; i += 1) {
    const empty = document.createElement("div");
    empty.className = "day-cell empty-cell";
    els.calendarGrid.appendChild(empty);
  }

  const today = todayStr();
  for (let day = 1; day <= daysInMonth; day += 1) {
    const date = formatLocalYmd(new Date(y, m, day));
    const weightKg = map.get(date);
    const hasRecord = Number.isFinite(weightKg);
    const isToday = date === today;
    const isFuture = date > today;

    const cell = document.createElement("button");
    cell.type = "button";
    cell.className = "day-cell";
    if (isToday) cell.classList.add("today");
    if (hasRecord) cell.classList.add("has-record");
    if (isFuture) cell.classList.add("future");
    cell.disabled = isFuture;

    const value = hasRecord
      ? (state.homeUnit === UNIT.KG ? weightKg : kgToJin(weightKg)).toFixed(2)
      : "";

    cell.innerHTML = `
      <span class="day-num">${day}</span>
      <span class="day-weight">${value}</span>
    `;
    cell.addEventListener("click", () => openAddDialog(date));

    els.calendarGrid.appendChild(cell);
  }
}

function visibleMonthRecords() {
  const y = state.currentMonth.getFullYear();
  const m = state.currentMonth.getMonth();
  const monthPrefix = `${y}-${String(m + 1).padStart(2, "0")}`;
  return state.records.filter((r) => r.date.startsWith(monthPrefix));
}

function exportExcel() {
  const rows = visibleMonthRecords().map((r) => ({
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
function firstDayOfMonth(d) { return new Date(d.getFullYear(), d.getMonth(), 1); }
function addMonth(base, offset) { return new Date(base.getFullYear(), base.getMonth() + offset, 1); }
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

/** 本地时间文件名用 yyyyMMdd_HHmmss */
function localDateTimeCompact() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}_${p(d.getHours())}${p(d.getMinutes())}${p(d.getSeconds())}`;
}
function isDateValid(date) {
  return /^\d{4}-\d{2}-\d{2}$/.test(date) && !Number.isNaN(new Date(date).getTime());
}
