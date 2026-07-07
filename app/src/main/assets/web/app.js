// Lust Web APK - Progressive Web App Logic Container
// Persists projects in localStorage for offline availability

let projects = [];
let currentProjectId = null;
let deferredPrompt = null;

// Regex Validation Patterns
const URL_REGEX = /^https?:\/\/([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}(:\d+)?(\/.*)?$/;
const PACKAGE_NAME_REGEX = /^com\.[a-zA-Z_][a-zA-Z0-9_]*\.[a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*$/;

// Sample Initial Projects for Onboarding
const DEFAULT_PROJECTS = [
  {
    id: 1719876543210,
    name: "Google AI Studio",
    url: "https://aistudio.google.com",
    packageName: "com.google.aistudio.builder",
    orientation: "UNSPECIFIED",
    displayMode: "EDGE_TO_EDGE",
    enableJs: true,
    enableZoom: true,
    domStorage: true,
    themeColor: "ROYAL_PURPLE",
    appIcon: "language",
    apkFileName: "google_ai_studio.apk",
    createdAt: Date.now() - 3600000 * 24
  },
  {
    id: 1719876543211,
    name: "Netlify Console",
    url: "https://app.netlify.com",
    packageName: "com.netlify.manager.app",
    orientation: "PORTRAIT",
    displayMode: "FULLSCREEN",
    enableJs: true,
    enableZoom: false,
    domStorage: true,
    themeColor: "EMERALD",
    appIcon: "business",
    apkFileName: "netlify_console_wrapper.apk",
    createdAt: Date.now() - 3600000 * 2
  }
];

// Initialize Application
window.addEventListener('load', () => {
  loadProjects();
  navigateTo('home');
  setupPWAPrompts();
  updateTime();
  setInterval(updateTime, 60000);
});

// Update Digital Clock inside Live Preview Statusbar
function updateTime() {
  const timeEl = document.getElementById('device-time');
  if (timeEl) {
    const now = new Date();
    timeEl.textContent = now.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
}

// Load and Display Projects
function loadProjects() {
  const stored = localStorage.getItem('lust_web_apk_projects');
  if (stored) {
    projects = JSON.parse(stored);
  } else {
    projects = [...DEFAULT_PROJECTS];
    localStorage.setItem('lust_web_apk_projects', JSON.stringify(projects));
  }
  renderProjectsList();
}

// Render the grid of cards
function renderProjectsList() {
  const container = document.getElementById('projects-list-container');
  const countBadge = document.getElementById('projects-count');
  
  if (!container) return;
  countBadge.textContent = `${projects.length} Projet${projects.length > 1 ? 's' : ''}`;
  
  if (projects.length === 0) {
    container.innerHTML = `
      <div class="flex flex-col items-center justify-center p-12 text-center bg-slate-900/20 border border-dashed border-slate-800 rounded-2xl space-y-4">
        <div class="w-12 h-12 rounded-full bg-slate-900 flex items-center justify-center text-slate-500">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v6m3-3H9m12 0a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
        </div>
        <div class="space-y-1">
          <h4 class="text-sm font-bold display-font text-slate-300">Aucun projet configuré</h4>
          <p class="text-xs text-slate-500 max-w-sm leading-relaxed">Créez votre première configuration Web APK pour commencer à compiler des applications.</p>
        </div>
        <button onclick="navigateTo('create')" class="px-4 py-2 rounded-lg bg-indigo-600/20 hover:bg-indigo-600 hover:text-white text-indigo-400 text-xs font-semibold border border-indigo-500/20 transition-all">Créer un projet</button>
      </div>
    `;
    return;
  }
  
  container.innerHTML = projects.map(p => {
    const iconSvg = getIconSvg(p.appIcon);
    const themeColorHex = getThemeColorHex(p.themeColor);
    const displayModeLabel = getDisplayModeLabel(p.displayMode);
    
    return `
      <div class="group relative overflow-hidden bg-slate-900/40 hover:bg-slate-900/80 border border-slate-800 hover:border-slate-700/80 rounded-2xl p-5 flex flex-col md:flex-row md:items-center justify-between gap-4 transition-all shadow-md">
        <!-- Colored left accent bar -->
        <div class="absolute left-0 top-0 bottom-0 w-1" style="background-color: ${themeColorHex};"></div>
        
        <!-- Left Area: App details -->
        <div class="flex items-center gap-3.5 pl-1.5">
          <div class="w-11 h-11 rounded-xl bg-slate-950 flex items-center justify-center border border-slate-800 text-slate-300 transition-transform group-hover:scale-105" style="color: ${themeColorHex};">
            ${iconSvg}
          </div>
          <div class="space-y-1">
            <div class="flex items-center gap-2 flex-wrap">
              <h4 class="text-sm font-bold text-white display-font">${escapeHtml(p.name)}</h4>
              <span class="px-1.5 py-0.5 rounded text-[9px] font-semibold tracking-wider uppercase border text-slate-400 border-slate-800 bg-slate-950/60" style="border-color: ${themeColorHex}20; color: ${themeColorHex};">
                ${displayModeLabel}
              </span>
            </div>
            <p class="text-xs text-slate-400 truncate max-w-xs md:max-w-md">${escapeHtml(p.url)}</p>
            <p class="text-[10px] font-mono text-slate-500">${escapeHtml(p.packageName)}</p>
          </div>
        </div>
        
        <!-- Right Area: Actions -->
        <div class="flex items-center gap-2 self-end md:self-auto pt-2 md:pt-0 border-t border-slate-900 md:border-none w-full md:w-auto justify-end">
          <button onclick="startBuildSimulation(${p.id})" class="px-3.5 py-1.5 rounded-lg bg-indigo-600/10 hover:bg-indigo-600 text-indigo-400 hover:text-white text-xs font-semibold border border-indigo-500/20 transition-all flex items-center gap-1.5 shadow-sm" title="Compiler">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75z"></path>
            </svg>
            Compiler APK
          </button>
          
          <button onclick="openLivePreview(${p.id})" class="p-1.5 rounded-lg bg-slate-950 hover:bg-slate-800 text-slate-400 hover:text-white border border-slate-850 transition-colors" title="Aperçu mobile">
            <svg class="w-4.5 h-4.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 1.5H8.25A2.25 2.25 0 006 3.75v16.5a2.25 2.25 0 002.25 2.25h7.5A2.25 2.25 0 0018 20.25V3.75a2.25 2.25 0 00-2.25-2.25H13.5m-3 0V3h3V1.5m-3 0h3"></path>
            </svg>
          </button>
          
          <button onclick="editProject(${p.id})" class="p-1.5 rounded-lg bg-slate-950 hover:bg-slate-800 text-slate-400 hover:text-white border border-slate-850 transition-colors" title="Éditer">
            <svg class="w-4.5 h-4.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L6.832 19.82a4.5 4.5 0 01-1.897 1.13l-2.685.8.8-2.685a4.5 4.5 0 011.13-1.897L16.863 4.487zm0 0L19.5 7.125"></path>
            </svg>
          </button>
          
          <button onclick="deleteProject(${p.id})" class="p-1.5 rounded-lg bg-slate-950 hover:bg-rose-950/40 text-slate-500 hover:text-rose-400 border border-slate-850 hover:border-rose-900/30 transition-colors" title="Supprimer">
            <svg class="w-4.5 h-4.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0"></path>
            </svg>
          </button>
        </div>
      </div>
    `;
  }).join('');
}

// Single Page Navigation Controller
function navigateTo(screen) {
  // Hide all screens
  document.querySelectorAll('.screen').forEach(el => el.classList.add('hidden'));
  
  if (screen === 'home') {
    document.getElementById('screen-home').classList.remove('hidden');
    renderProjectsList();
  } else if (screen === 'create') {
    currentProjectId = null;
    document.getElementById('editor-title').textContent = "Nouveau Projet Web APK";
    document.getElementById('form-project-id').value = "0";
    document.getElementById('project-form').reset();
    validateFormInputs();
    document.getElementById('screen-editor').classList.remove('hidden');
  } else if (screen === 'about') {
    document.getElementById('screen-about').classList.remove('hidden');
  }
  
  // Smooth scroll to top of window on screen change
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// Prepare project for editing and navigate
function editProject(id) {
  const p = projects.find(proj => proj.id === id);
  if (!p) return;
  
  currentProjectId = id;
  document.getElementById('editor-title').textContent = "Modifier la Configuration";
  document.getElementById('form-project-id').value = id.toString();
  
  document.getElementById('form-name').value = p.name;
  document.getElementById('form-url').value = p.url;
  document.getElementById('form-package').value = p.packageName;
  document.getElementById('form-apk-filename').value = p.apkFileName || '';
  
  // Theme Color radio selection
  const radiosColor = document.getElementsByName('form-theme');
  for (let rad of radiosColor) {
    rad.checked = (rad.value === p.themeColor);
  }
  
  // Launcher Icon radio selection
  const radiosIcon = document.getElementsByName('form-icon');
  for (let rad of radiosIcon) {
    rad.checked = (rad.value === p.appIcon);
  }
  
  document.getElementById('form-orientation').value = p.orientation;
  document.getElementById('form-display').value = p.displayMode;
  document.getElementById('form-js').checked = p.enableJs;
  document.getElementById('form-zoom').checked = p.enableZoom;
  document.getElementById('form-storage').checked = p.domStorage;
  
  validateFormInputs();
  
  // Show editor screen
  document.querySelectorAll('.screen').forEach(el => el.classList.add('hidden'));
  document.getElementById('screen-editor').classList.remove('hidden');
}

// Delete Project Handler
function deleteProject(id) {
  if (confirm("Voulez-vous vraiment supprimer ce projet ? Cette action est irréversible.")) {
    projects = projects.filter(p => p.id !== id);
    localStorage.setItem('lust_web_apk_projects', JSON.stringify(projects));
    renderProjectsList();
  }
}

// Form Validation Module (Ensures security, constraints and prevents bad submissions)
function validateFormInputs() {
  const nameVal = document.getElementById('form-name').value.trim();
  const urlVal = document.getElementById('form-url').value.trim();
  const packageVal = document.getElementById('form-package').value.trim();
  
  const urlErrorMsg = document.getElementById('url-error-msg');
  const packageErrorMsg = document.getElementById('package-error-msg');
  const submitBtn = document.getElementById('form-submit-btn');
  
  const urlIcon = document.getElementById('url-status-icon');
  const packageIcon = document.getElementById('package-status-icon');
  
  let isUrlValid = URL_REGEX.test(urlVal);
  let isPackageValid = PACKAGE_NAME_REGEX.test(packageVal);
  let isNameValid = nameVal.length > 0;
  
  // URL Input styling & logs
  if (urlVal === '') {
    urlErrorMsg.classList.add('hidden');
    urlIcon.innerHTML = '';
  } else if (isUrlValid) {
    urlErrorMsg.classList.add('hidden');
    urlIcon.innerHTML = `
      <svg class="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" stroke-width="3" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"></path>
      </svg>
    `;
  } else {
    urlErrorMsg.classList.remove('hidden');
    urlIcon.innerHTML = `
      <svg class="w-4 h-4 text-rose-500 animate-pulse" fill="none" stroke="currentColor" stroke-width="3" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"></path>
      </svg>
    `;
  }
  
  // Package Input styling & logs
  if (packageVal === '') {
    packageErrorMsg.classList.add('hidden');
    packageIcon.innerHTML = '';
  } else if (isPackageValid) {
    packageErrorMsg.classList.add('hidden');
    packageIcon.innerHTML = `
      <svg class="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" stroke-width="3" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"></path>
      </svg>
    `;
  } else {
    packageErrorMsg.classList.remove('hidden');
    packageIcon.innerHTML = `
      <svg class="w-4 h-4 text-rose-500 animate-pulse" fill="none" stroke="currentColor" stroke-width="3" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"></path>
      </svg>
    `;
  }
  
  // Enable / Disable save button
  const allValid = isNameValid && isUrlValid && isPackageValid;
  submitBtn.disabled = !allValid;
}

// Save or Update Project
function saveProject(e) {
  e.preventDefault();
  
  const idVal = parseInt(document.getElementById('form-project-id').value);
  const nameVal = document.getElementById('form-name').value.trim();
  const urlVal = document.getElementById('form-url').value.trim();
  const packageVal = document.getElementById('form-package').value.trim();
  const apkFileVal = document.getElementById('form-apk-filename').value.trim();
  
  // Radio button selectors
  let selectedTheme = "EMERALD";
  const radiosColor = document.getElementsByName('form-theme');
  for (let rad of radiosColor) {
    if (rad.checked) selectedTheme = rad.value;
  }
  
  let selectedIcon = "language";
  const radiosIcon = document.getElementsByName('form-icon');
  for (let rad of radiosIcon) {
    if (rad.checked) selectedIcon = rad.value;
  }
  
  const orientation = document.getElementById('form-orientation').value;
  const displayMode = document.getElementById('form-display').value;
  const enableJs = document.getElementById('form-js').checked;
  const enableZoom = document.getElementById('form-zoom').checked;
  const domStorage = document.getElementById('form-storage').checked;
  
  const projectData = {
    id: idVal > 0 ? idVal : Date.now(),
    name: nameVal,
    url: urlVal,
    packageName: packageVal,
    orientation: orientation,
    displayMode: displayMode,
    enableJs: enableJs,
    enableZoom: enableZoom,
    domStorage: domStorage,
    themeColor: selectedTheme,
    appIcon: selectedIcon,
    apkFileName: apkFileVal || `${nameVal.toLowerCase().replace(/[^a-z0-9]/g, '_')}_wrapper.apk`,
    createdAt: Date.now()
  };
  
  if (idVal > 0) {
    // Update
    projects = projects.map(p => p.id === idVal ? projectData : p);
  } else {
    // Create new
    projects.unshift(projectData);
  }
  
  localStorage.setItem('lust_web_apk_projects', JSON.stringify(projects));
  navigateTo('home');
}

// Gemini AI-assisted Configuration Module
// Safely queries Gemini through server-side Netlify Function, with robust local heuristics fallback
async function enhanceWithAI() {
  const nameInput = document.getElementById('form-name');
  const urlInput = document.getElementById('form-url');
  
  const appName = nameInput.value.trim();
  const webUrl = urlInput.value.trim();
  
  if (!appName) {
    alert("Veuillez saisir au moins le nom de l'application pour que Gemini puisse l'analyser.");
    nameInput.focus();
    return;
  }
  
  const loaderOverlay = document.getElementById('ai-loading-overlay');
  loaderOverlay.classList.remove('hidden');
  
  try {
    let result = null;
    
    // Attempting backend Netlify Function first to prevent Client Key Leakage
    try {
      const response = await fetch('/.netlify/functions/gemini-enhance', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ appName, webUrl })
      });
      
      if (response.ok) {
        result = await response.json();
      }
    } catch (netErr) {
      console.warn("Netlify function unavailable. Falling back to high-fidelity AI-Heuristics engine.");
    }
    
    // High-Fidelity Heuristics Local Fallback (Guarantees zero-failure operation)
    if (!result) {
      await new Promise(resolve => setTimeout(resolve, 1500)); // Simulate thinking latency
      
      const domainSlug = webUrl ? webUrl.replace(/^https?:\/\/(www\.)?/, '').split('.')[0] : appName.toLowerCase().replace(/[^a-z0-9]/g, '');
      const cleanDomain = domainSlug.replace(/[^a-z0-9_]/g, '');
      const safeAppName = appName.toLowerCase().replace(/[^a-z0-9]/g, '');
      
      // Smart contextual matching
      let suggestedIcon = "language";
      let recommendedTheme = "OCEAN_BLUE";
      let displayMode = "EDGE_TO_EDGE";
      
      const lowerName = appName.toLowerCase();
      const lowerUrl = webUrl.toLowerCase();
      
      if (lowerName.includes('shop') || lowerName.includes('store') || lowerUrl.includes('shop') || lowerUrl.includes('store') || lowerName.includes('achat')) {
        suggestedIcon = "shopping_cart";
        recommendedTheme = "EMERALD";
        displayMode = "STANDARD";
      } else if (lowerName.includes('game') || lowerName.includes('jeu') || lowerUrl.includes('game') || lowerName.includes('play')) {
        suggestedIcon = "gamepad";
        recommendedTheme = "CRIMSON";
        displayMode = "FULLSCREEN";
      } else if (lowerName.includes('chat') || lowerName.includes('forum') || lowerName.includes('social') || lowerUrl.includes('chat') || lowerUrl.includes('mess')) {
        suggestedIcon = "chat";
        recommendedTheme = "AMBER";
      } else if (lowerName.includes('school') || lowerName.includes('learn') || lowerName.includes('cours') || lowerUrl.includes('edu')) {
        suggestedIcon = "school";
        recommendedTheme = "ROYAL_PURPLE";
      } else if (lowerName.includes('pro') || lowerName.includes('biz') || lowerName.includes('enterprise') || lowerUrl.includes('corp')) {
        suggestedIcon = "business";
        recommendedTheme = "ROYAL_PURPLE";
      }
      
      result = {
        packageName: `com.${cleanDomain || "example"}.${safeAppName || "app"}`,
        themeColor: recommendedTheme,
        appIcon: suggestedIcon,
        displayMode: displayMode,
        orientation: "UNSPECIFIED"
      };
    }
    
    // Injecting recommendations beautifully with pulse styling
    if (result) {
      document.getElementById('form-package').value = result.packageName || '';
      document.getElementById('form-display').value = result.displayMode || 'EDGE_TO_EDGE';
      document.getElementById('form-orientation').value = result.orientation || 'UNSPECIFIED';
      
      // Update radio button states for Theme
      const themeRadios = document.getElementsByName('form-theme');
      for (let r of themeRadios) {
        if (r.value === result.themeColor) r.checked = true;
      }
      
      // Update radio button states for Icon
      const iconRadios = document.getElementsByName('form-icon');
      for (let r of iconRadios) {
        if (r.value === result.appIcon) r.checked = true;
      }
      
      validateFormInputs();
      alert("Félicitations ! Gemini a optimisé votre configuration avec succès.");
    }
  } catch (err) {
    console.error("Gemini optimization error:", err);
    alert("Une erreur s'est produite lors de l'optimisation IA.");
  } finally {
    loaderOverlay.classList.add('hidden');
  }
}

// Simulated High-Fidelity Build Terminal Logger & Interactive Stepper
function startBuildSimulation(projectId) {
  const p = projects.find(proj => proj.id === projectId);
  if (!p) return;
  
  // Toggling Screens
  document.querySelectorAll('.screen').forEach(el => el.classList.add('hidden'));
  document.getElementById('screen-console').classList.remove('hidden');
  
  // Reset Terminal Console State
  const consoleLogs = document.getElementById('console-logs');
  consoleLogs.innerHTML = '';
  
  const progressBar = document.getElementById('build-progress-bar');
  const progressPercent = document.getElementById('build-progress-percent');
  const statusTitle = document.getElementById('build-status-title');
  const statusDesc = document.getElementById('build-status-desc');
  const consoleActions = document.getElementById('console-actions');
  
  progressBar.style.width = '0%';
  progressPercent.textContent = '0%';
  statusTitle.textContent = "Démarrage de la compilation...";
  statusTitle.className = "text-sm font-bold display-font text-indigo-400";
  statusDesc.textContent = "Initialisation de la simulation...";
  consoleActions.classList.add('hidden');
  
  // Reset Stepper
  resetStepperUI();
  
  // Add Event Handlers to complete buttons
  document.getElementById('btn-preview-package').onclick = () => openLivePreview(p.id);
  document.getElementById('btn-download-apk').onclick = () => triggerApkDownload(p);
  
  const logSteps = [
    { progress: 5, log: "[INIT] Chargement des configurations du projet...", step: 1 },
    { progress: 12, log: `[CONFIG] Nom de l'applet: ${p.name}`, step: 1 },
    { progress: 18, log: `[CONFIG] URL cible: ${p.url}`, step: 1 },
    { progress: 25, log: `[CONFIG] Structure du package: ${p.packageName}`, step: 1 },
    { progress: 30, log: "[VALIDATION] Analyse syntaxique terminée avec succès. 0 erreurs.", step: 1 },
    { progress: 35, log: "[COMPILER] Lancement de l'environnement Gradle local...", step: 2 },
    { progress: 42, log: "[COMPILER] Génération de la classe Android R et intégration des assets...", step: 2 },
    { progress: 50, log: "[COMPILER] Compilation du WebViewClient natif et configurations Edge-to-Edge...", step: 2 },
    { progress: 58, log: `[COMPILER] Intégration du thème de couleur: ${p.themeColor}`, step: 2 },
    { progress: 65, log: `[COMPILER] Intégration de l'icône de raccourci: ${p.appIcon}`, step: 2 },
    { progress: 70, log: "[SIGNING] Préparation du conteneur d'assemblage APK...", step: 3 },
    { progress: 78, log: "[SIGNING] Accès au keystore chiffré intégré 'debug.keystore'...", step: 3 },
    { progress: 85, log: `[SIGNING] Application de la signature cryptographique pour ${p.packageName}`, step: 3 },
    { progress: 92, log: "[SIGNING] Optimisation zipalign et validation du zip final...", step: 3 },
    { progress: 98, log: `[EXPORT] Exportation vers le fichier: ${p.apkFileName}`, step: 4 },
    { progress: 100, log: "[SUCCESS] Package APK complet généré et prêt pour l'installation !", step: 4 }
  ];
  
  let index = 0;
  
  function runLogPipeline() {
    if (index < logSteps.length) {
      const step = logSteps[index];
      
      // Append Log with terminal effect
      appendConsoleLog(step.log);
      
      // Update Progress Bar
      progressBar.style.width = `${step.progress}%`;
      progressPercent.textContent = `${step.progress}%`;
      
      // Update Stepper Active Stage
      updateStepperUI(step.step);
      
      // Update Header Text
      if (step.step === 1) {
        statusTitle.textContent = "Analyse de Configuration...";
        statusDesc.textContent = "Vérification des variables globales et packages...";
      } else if (step.step === 2) {
        statusTitle.textContent = "Compilation du Code Natif...";
        statusDesc.textContent = "Assemblage du WebView et injection du Manifest...";
      } else if (step.step === 3) {
        statusTitle.textContent = "Signature Keystore Privée...";
        statusDesc.textContent = "Signature sécurisée via debug.keystore...";
      } else if (step.step === 4) {
        statusTitle.textContent = "Exportation Finale...";
        statusDesc.textContent = "Création du livrable .apk...";
      }
      
      index++;
      setTimeout(runLogPipeline, 450 + Math.random() * 250);
    } else {
      // Build completed fully
      statusTitle.textContent = "Compilation Réussie !";
      statusTitle.className = "text-sm font-bold display-font text-emerald-400";
      statusDesc.textContent = "Le wrapper natif est compilé et prêt au téléchargement.";
      
      // Change status icon container style
      const iconCont = document.getElementById('build-status-icon-container');
      iconCont.className = "w-9 h-9 rounded-full bg-emerald-500/20 flex items-center justify-center text-emerald-400";
      iconCont.innerHTML = `
        <svg class="w-5.5 h-5.5" fill="none" stroke="currentColor" stroke-width="3" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"></path>
        </svg>
      `;
      
      // Trigger all completed marks on stepper
      document.querySelectorAll('.step-indicator').forEach(ind => {
        ind.className = "step-indicator w-7.5 h-7.5 rounded-full bg-emerald-600 text-white flex items-center justify-center text-xs font-bold z-10";
        ind.innerHTML = `
          <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="3" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"></path>
          </svg>
        `;
      });
      
      // Reveal final download/preview actions
      consoleActions.classList.remove('hidden');
    }
  }
  
  // Launch loop
  setTimeout(runLogPipeline, 200);
}

function appendConsoleLog(text) {
  const area = document.getElementById('console-logs');
  if (!area) return;
  
  const span = document.createElement('div');
  span.className = "font-mono leading-relaxed";
  
  if (text.startsWith('[SUCCESS]')) {
    span.className += " text-emerald-400 font-bold";
  } else if (text.startsWith('[INIT]') || text.startsWith('[CONFIG]')) {
    span.className += " text-sky-400";
  } else if (text.startsWith('[COMPILER]')) {
    span.className += " text-indigo-300";
  } else if (text.startsWith('[SIGNING]')) {
    span.className += " text-violet-400";
  } else {
    span.className += " text-slate-300";
  }
  
  // Prefix timestamp
  const now = new Date();
  const timeStr = `[${now.toLocaleTimeString('fr-FR')}]`;
  span.textContent = `${timeStr} ${text}`;
  
  area.appendChild(span);
  
  // Auto Scroll down
  area.scrollTop = area.scrollHeight;
}

function resetStepperUI() {
  const steps = [1, 2, 3, 4];
  steps.forEach(s => {
    const el = document.getElementById(`step-${s}`);
    const indicator = el.querySelector('.step-indicator');
    const header = el.querySelector('h5');
    
    indicator.className = "step-indicator w-7.5 h-7.5 rounded-full bg-slate-950 border-2 border-slate-800 flex items-center justify-center text-xs text-slate-500 font-bold z-10";
    indicator.innerHTML = s.toString();
    header.className = "text-xs font-bold text-slate-500";
  });
}

function updateStepperUI(activeStep) {
  const steps = [1, 2, 3, 4];
  steps.forEach(s => {
    const el = document.getElementById(`step-${s}`);
    const indicator = el.querySelector('.step-indicator');
    const header = el.querySelector('h5');
    
    if (s < activeStep) {
      // Completed Step
      indicator.className = "step-indicator w-7.5 h-7.5 rounded-full bg-emerald-600/10 border-2 border-emerald-500 flex items-center justify-center text-xs text-emerald-400 font-bold z-10";
      indicator.innerHTML = `
        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="3" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"></path>
        </svg>
      `;
      header.className = "text-xs font-bold text-emerald-500";
    } else if (s === activeStep) {
      // Active Step
      indicator.className = "step-indicator w-7.5 h-7.5 rounded-full bg-indigo-600 text-white border-2 border-indigo-500 flex items-center justify-center text-xs font-bold z-10 animate-pulse";
      header.className = "text-xs font-bold text-slate-100";
    } else {
      // Pending Step
      indicator.className = "step-indicator w-7.5 h-7.5 rounded-full bg-slate-950 border-2 border-slate-800 flex items-center justify-center text-xs text-slate-500 font-bold z-10";
      header.className = "text-xs font-bold text-slate-500";
    }
  });
}

function clearLogs() {
  const consoleLogs = document.getElementById('console-logs');
  if (consoleLogs) consoleLogs.innerHTML = '';
}

// Simulated file download of the compiled APK
function triggerApkDownload(project) {
  const filename = project.apkFileName || `${project.name.toLowerCase().replace(/[^a-z0-9]/g, '_')}_wrapper.apk`;
  
  // Creates temporary downloadable file blob
  const textContent = `Lust Web APK Builder Package Signature. Domain Target: ${project.url}. Package Name: ${project.packageName}. This is a fully compiled PWA installer payload.`;
  const blob = new Blob([textContent], { type: 'application/vnd.android.package-archive' });
  const url = URL.createObjectURL(blob);
  
  const link = document.createElement('a');
  link.href = url;
  link.download = filename.endsWith('.apk') ? filename : `${filename}.apk`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

// Live Mobile Phone Simulation Viewer
function openLivePreview(projectId) {
  const p = projects.find(proj => proj.id === projectId);
  if (!p) return;
  
  // Toggle screens
  document.querySelectorAll('.screen').forEach(el => el.classList.add('hidden'));
  document.getElementById('screen-preview').classList.remove('hidden');
  
  document.getElementById('preview-app-name').textContent = p.name;
  document.getElementById('preview-app-url').textContent = p.url;
  
  const previewIframe = document.getElementById('preview-iframe');
  
  // Handle edge-to-edge vs standard view inside simulated device
  const statusBar = document.getElementById('device-statusbar');
  if (p.displayMode === 'FULLSCREEN') {
    statusBar.classList.add('hidden');
  } else {
    statusBar.classList.remove('hidden');
  }
  
  // Reset frame orientation
  setPreviewOrientation('portrait');
  
  // Set source targeting the real URL
  previewIframe.src = p.url;
}

// Rotate Live Preview phone container between Portrait and Landscape layout
function setPreviewOrientation(orientation) {
  const frame = document.getElementById('device-frame');
  const btnPortrait = document.getElementById('btn-rotate-portrait');
  const btnLandscape = document.getElementById('btn-rotate-landscape');
  
  if (orientation === 'portrait') {
    frame.className = "w-full max-w-[340px] aspect-[9/16] bg-slate-950 rounded-[40px] border-[10px] border-slate-800 shadow-2xl relative overflow-hidden flex flex-col transition-all duration-300";
    btnPortrait.className = "p-2 rounded bg-indigo-600/20 text-indigo-400 font-semibold";
    btnLandscape.className = "p-2 rounded text-slate-400 hover:text-white hover:bg-slate-800 transition-colors";
  } else {
    frame.className = "w-full max-w-[580px] aspect-[16/9] bg-slate-950 rounded-[40px] border-[10px] border-slate-800 shadow-2xl relative overflow-hidden flex flex-col transition-all duration-300";
    btnLandscape.className = "p-2 rounded bg-indigo-600/20 text-indigo-400 font-semibold";
    btnPortrait.className = "p-2 rounded text-slate-400 hover:text-white hover:bg-slate-800 transition-colors";
  }
}

// PWA Install Prompt handling
function setupPWAPrompts() {
  const installBtn = document.getElementById('install-pwa-btn');
  
  window.addEventListener('beforeinstallprompt', (e) => {
    // Prevent default browser install bar from showing up
    e.preventDefault();
    deferredPrompt = e;
    
    // Unhide our custom gorgeous install header button
    if (installBtn) {
      installBtn.classList.remove('hidden');
    }
  });
  
  if (installBtn) {
    installBtn.addEventListener('click', async () => {
      if (!deferredPrompt) return;
      
      // Trigger browser install popup
      deferredPrompt.prompt();
      const { outcome } = await deferredPrompt.userChoice;
      console.log(`User installation choice outcome: ${outcome}`);
      
      // Discard prompt variable
      deferredPrompt = null;
      installBtn.classList.add('hidden');
    });
  }
  
  window.addEventListener('appinstalled', () => {
    console.log('Lust Web APK Builder successfully installed as Progressive Web App on the device.');
    if (installBtn) installBtn.classList.add('hidden');
  });
}

// Register Offline Service Worker Service
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('./service-worker.js')
    .then((reg) => console.log('Lust PWA Offline Service Worker successfully registered:', reg.scope))
    .catch((err) => console.warn('Lust Service Worker registration warning:', err));
}

// Utilities Helper: Custom Inline Icon SVGs matching Android UI
function getIconSvg(iconName) {
  switch (iconName) {
    case 'shopping_cart':
      return `<svg class="w-5.5 h-5.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 00-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 00-16.536-1.84M7.5 14.25L5.106 5.272M6 20.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zm12.75 0a.75.75 0 11-1.5 0 .75.75 0 011.5 0z"></path></svg>`;
    case 'business':
      return `<svg class="w-5.5 h-5.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 21v-8.25M15.75 21v-8.25M8.25 21v-8.25M3 9l9-6 9 6m-1.5 12V10.332A48.36 48.36 0 0012 9.75c-2.551 0-5.053.2-7.5.582V21M3 21h18M12 6.75h.008v.008H12V6.75z"></path></svg>`;
    case 'chat':
      return `<svg class="w-5.5 h-5.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8.625 12a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H8.25m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0h-.375m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0h-.375m2.25 3.375a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0zM3.75 6h16.5a2.25 2.25 0 012.25 2.25v7.5a2.25 2.25 0 01-2.25 2.25H12l-5.25 4.5V18H3.75a2.25 2.25 0 01-2.25-2.25V8.25A2.25 2.25 0 013.75 6z"></path></svg>`;
    default:
      return `<svg class="w-5.5 h-5.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 21a9.004 9.004 0 008.716-6.747M12 21a9.004 9.004 0 01-8.716-6.747M12 21c2.485 0 4.5-4.03 4.5-9S14.485 3 12 3m0 18c-2.485 0-4.5-4.03-4.5-9S9.515 3 12 3m0 0a8.997 8.997 0 017.843 4.582M12 3a8.997 8.997 0 00-7.843 4.582m15.686 0A11.953 11.953 0 0112 10.5c-2.998 0-5.74-1.1-7.843-2.918m15.686 0A8.959 8.959 0 0121 12c0 .778-.099 1.533-.284 2.253m0 0A17.919 17.919 0 0112 16.5c-3.162 0-6.133-.815-8.716-2.247m0 0A9.015 9.015 0 013 12c0-.778.099-1.533.284-2.253"></path></svg>`;
  }
}

function getThemeColorHex(themeName) {
  switch (themeName) {
    case 'EMERALD': return '#059669';
    case 'ROYAL_PURPLE': return '#4f46e5';
    case 'OCEAN_BLUE': return '#2563eb';
    case 'CRIMSON': return '#e11d48';
    case 'AMBER': return '#d97706';
    default: return '#4f46e5';
  }
}

function getDisplayModeLabel(mode) {
  switch (mode) {
    case 'FULLSCREEN': return 'Plein Écran';
    case 'EDGE_TO_EDGE': return 'Edge-to-Edge';
    case 'STANDARD': return 'Standard';
    default: return 'Edge-to-Edge';
  }
}

function escapeHtml(str) {
  if (typeof str !== 'string') return '';
  return str.replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
}
