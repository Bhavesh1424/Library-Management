/**
 * Athenaeum CampusLib Exchange - Interactive Enterprise Frontend
 * Connects to Spring Boot 3.2 REST API with JWT Auth, Pessimistic Locking, and Real-time Status Tracking.
 */

// Global State
const state = {
    user: null,
    authModalCanClose: false,
    catalog: [],
    branches: [],
    currentTab: 'catalog',
    currentSubTab: 'dispatch',
    searchDebounceTimer: null,
    selectedBookForTransfer: null,
    cachedInventory: {},
    catalogBranchFilter: 'ALL',
    catalogAvailableOnly: false,
    librarianBranchFilter: '',
    allAuditLogs: []
};

// Demo Personas for One-Click Testing
const PERSONAS = {
    student: { email: 'student@campus.edu', password: 'password123', label: 'Aarav Sharma (Student)', role: 'ROLE_STUDENT', initial: 'A' },
    cs_librarian: { email: 'cs.librarian@campus.edu', password: 'password123', label: 'Priya Nair (CS Desk)', role: 'ROLE_LIBRARIAN', initial: 'P' },
    central_librarian: { email: 'central.librarian@campus.edu', password: 'password123', label: 'Rajesh Kumar (Central Desk)', role: 'ROLE_LIBRARIAN', initial: 'R' },
    admin: { email: 'admin@campus.edu', password: 'admin123', label: 'Chief Admin', role: 'ROLE_ADMIN', initial: 'C' }
};

// Book cover styling palettes for calm, aesthetic variety
const COVER_THEMES = [
    { bg: 'bg-[#FDE8D3]/80', border: 'border-[#FDE8D3]', text: 'text-tertiary', icon: 'auto_stories', series: 'MIT Press Core' },
    { bg: 'bg-[#99CDD8]/25', border: 'border-[#99CDD8]/50', text: 'text-secondary', icon: 'memory', series: 'Systems Series' },
    { bg: 'bg-[#DAEBE3]/60', border: 'border-[#DAEBE3]', text: 'text-primary', icon: 'storage', series: 'Data & Stacks' },
    { bg: 'bg-[#F3C3B2]/35', border: 'border-[#F3C3B2]/60', text: 'text-tertiary', icon: 'water_drop', series: 'Engineering Lab' },
    { bg: 'bg-[#e4e2de]/60', border: 'border-[#c4c8c1]/60', text: 'text-on-surface-variant', icon: 'hub', series: 'Collegiate Press' }
];

// ================= HERO VIDEO FRAME CANVAS PLAYER =================
const heroPlayer = {
    canvas: null,
    ctx: null,
    totalFrames: 240,
    frames: [],
    loadedCount: 0,
    currentFrame: 0,
    isPlaying: true,
    lastFrameTime: 0,
    frameInterval: 41.6, // ~24 fps
    animationReqId: null
};

function initHeroCanvasPlayer() {
    heroPlayer.canvas = document.getElementById('hero-frame-canvas');
    if (!heroPlayer.canvas) return;
    heroPlayer.ctx = heroPlayer.canvas.getContext('2d');

    // Preload all 240 frames
    for (let i = 1; i <= heroPlayer.totalFrames; i++) {
        const img = new Image();
        const frameNum = String(i).padStart(3, '0');
        img.src = `/frames/ezgif-frame-${frameNum}.jpg`;
        img.onload = () => {
            heroPlayer.loadedCount++;
            if (i === 1 && heroPlayer.currentFrame === 0) {
                renderCurrentHeroFrame();
            }
        };
        heroPlayer.frames.push(img);
    }

    // Resize canvas to match display size
    window.addEventListener('resize', resizeHeroCanvas);
    resizeHeroCanvas();

    // Start smooth playback loop
    requestAnimationFrame(heroAnimationLoop);
}

function resizeHeroCanvas() {
    if (!heroPlayer.canvas) return;
    const rect = heroPlayer.canvas.getBoundingClientRect();
    if (rect.width > 0 && rect.height > 0) {
        heroPlayer.canvas.width = Math.round(rect.width);
        heroPlayer.canvas.height = Math.round(rect.height);
        renderCurrentHeroFrame();
    }
}

function heroAnimationLoop(timestamp) {
    if (heroPlayer.isPlaying) {
        if (!heroPlayer.lastFrameTime) heroPlayer.lastFrameTime = timestamp;
        const elapsed = timestamp - heroPlayer.lastFrameTime;
        if (elapsed >= heroPlayer.frameInterval) {
            heroPlayer.currentFrame = (heroPlayer.currentFrame + 1) % heroPlayer.totalFrames;
            heroPlayer.lastFrameTime = timestamp;
            renderCurrentHeroFrame();
        }
    }
    heroPlayer.animationReqId = requestAnimationFrame(heroAnimationLoop);
}

function renderCurrentHeroFrame() {
    if (!heroPlayer.ctx || !heroPlayer.canvas) return;
    const img = heroPlayer.frames[heroPlayer.currentFrame];
    if (!img || !img.complete || img.naturalWidth === 0) return;

    const cw = heroPlayer.canvas.width;
    const ch = heroPlayer.canvas.height;
    const iw = img.naturalWidth;
    const ih = img.naturalHeight;

    // Aspect-cover fit to fill canvas without distortion
    const scale = Math.max(cw / iw, ch / ih);
    const nw = iw * scale;
    const nh = ih * scale;
    const ox = (cw - nw) / 2;
    const oy = (ch - nh) / 2;

    heroPlayer.ctx.clearRect(0, 0, cw, ch);
    heroPlayer.ctx.drawImage(img, ox, oy, nw, nh);
}

function toggleHeroPlayback() {
    heroPlayer.isPlaying = !heroPlayer.isPlaying;
    const icon = document.getElementById('hero-play-icon');
    if (icon) {
        icon.textContent = heroPlayer.isPlaying ? 'pause' : 'play_arrow';
    }
}

function scrollToCatalog(e) {
    if (e) e.preventDefault();
    const section = document.getElementById('catalog-search-section');
    if (section) {
        section.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

// ================= INITIALIZATION =================
document.addEventListener('DOMContentLoaded', async () => {
    await loadBranches();
    await loadCatalog('');
    initHeroCanvasPlayer();

    // Restore custom student label if previously registered/logged in
    const activeStudent = getActiveStudentProfile();
    if (activeStudent) {
        updateStudentChipLabel(activeStudent);
    } else {
        const chipText = document.getElementById('chip-student-name');
        if (chipText) chipText.textContent = 'Student (Aarav)';
    }
    const savedUserJson = localStorage.getItem('campus_lib_user');
    if (savedUserJson) {
        try {
            const savedUser = JSON.parse(savedUserJson);
            if (savedUser && savedUser.token) {
                applyAuthenticatedUserProfile(savedUser);
            } else {
                localStorage.removeItem('campus_lib_user');
                openAuthModal(false);
                switchAuthTab('login');
            }
        } catch (e) {
            localStorage.removeItem('campus_lib_user');
            openAuthModal(false);
            switchAuthTab('login');
        }
    } else {
        // A user MUST sign in or create an ID to begin
        openAuthModal(false);
        switchAuthTab('login');
    }

    // Global keyboard navigation & shortcuts (⌘K / Ctrl+K and Escape)
    window.addEventListener('keydown', (e) => {
        if ((e.metaKey || e.ctrlKey) && (e.key === 'k' || e.key === 'K')) {
            e.preventDefault();
            showTab('catalog');
            const input = document.getElementById('catalog-search-input');
            if (input) {
                input.focus();
                input.select();
                scrollToCatalog();
            }
        } else if (e.key === 'Escape') {
            closeTransferModal();
            if (state.authModalCanClose) {
                closeAuthModal();
            }
        }
    });

    // Start 48-hour hold countdown ticker (ticks every 1000ms)
    setInterval(tickHoldTimers, 1000);
});

// ================= AUTHENTICATION MODAL & ONBOARDING CONTROLS =================
function openAuthModal(canClose = true) {
    state.authModalCanClose = canClose;
    const modal = document.getElementById('auth-modal');
    const closeBtn = document.getElementById('auth-modal-close-btn');

    if (closeBtn) {
        if (!canClose) {
            closeBtn.classList.add('hidden');
        } else {
            closeBtn.classList.remove('hidden');
        }
    }

    if (modal) {
        modal.classList.add('open');
    }

    // Focus initial input if registering
    const regInput = document.getElementById('reg-university-id');
    if (regInput && !regInput.closest('form').classList.contains('hidden')) {
        setTimeout(() => regInput.focus(), 150);
    }
}

function closeAuthModal(force = false) {
    if (!force && !state.user && !state.authModalCanClose) {
        showToast('Please create your login ID or sign in to enter the library.', 'info');
        return;
    }
    const modal = document.getElementById('auth-modal');
    if (modal) {
        modal.classList.remove('open');
    }
}

function handleAuthModalBackdropClick(e) {
    const modal = document.getElementById('auth-modal');
    if (e.target === modal) {
        if (state.authModalCanClose) {
            closeAuthModal();
        } else {
            showToast('Please create your login ID or sign in to access the library.', 'info');
        }
    }
}

function switchAuthTab(tabName) {
    document.querySelectorAll('.auth-pill').forEach(btn => btn.classList.remove('active'));
    const activeBtn = document.getElementById(`auth-tab-${tabName}`);
    if (activeBtn) activeBtn.classList.add('active');

    const formReg = document.getElementById('form-register');
    const formLogin = document.getElementById('form-login');
    const paneDemo = document.getElementById('pane-demo');

    if (formReg) formReg.classList.toggle('hidden', tabName !== 'register');
    if (formLogin) formLogin.classList.toggle('hidden', tabName !== 'login');
    if (paneDemo) paneDemo.classList.toggle('hidden', tabName !== 'demo');

    if (tabName === 'login') {
        const identInput = document.getElementById('login-identifier');
        if (identInput) setTimeout(() => identInput.focus(), 150);
    } else if (tabName === 'register') {
        const regInput = document.getElementById('reg-university-id');
        if (regInput) setTimeout(() => regInput.focus(), 150);
    }
}

// User Registration Flow (User creates custom Login ID, Full Name, Email, and Password)
async function handleUserRegistration(event) {
    if (event) event.preventDefault();

    const univIdInput = document.getElementById('reg-university-id');
    const fullNameInput = document.getElementById('reg-fullname');
    const emailInput = document.getElementById('reg-email');
    const passInput = document.getElementById('reg-password');
    const submitBtn = document.getElementById('btn-submit-register');

    const universityId = univIdInput ? univIdInput.value.trim() : '';
    const fullName = fullNameInput ? fullNameInput.value.trim() : '';
    const email = emailInput ? emailInput.value.trim() : '';
    const password = passInput ? passInput.value : '';

    if (!universityId || !fullName || !email || !password) {
        showToast('Please fill in all registration fields', 'error');
        return;
    }

    if (password.length < 6) {
        showToast('Password must be at least 6 characters long', 'error');
        return;
    }

    const origBtnHtml = submitBtn ? submitBtn.innerHTML : '';
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="material-symbols-outlined text-base animate-spin" data-icon="sync">sync</span> Creating Campus ID...';
    }

    try {
        const response = await fetch('/api/v1/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                universityId,
                fullName,
                email,
                password
            })
        });

        const data = await response.json();

        if (!response.ok) {
            const errorMsg = data.message || data.error || (data.errors ? Object.values(data.errors).join(', ') : 'Registration failed');
            showToast(errorMsg, 'error');
            return;
        }

        // Successfully registered and received JWT token!
        localStorage.setItem('campus_lib_user', JSON.stringify(data));
        applyAuthenticatedUserProfile(data);

        // Reset inputs
        if (univIdInput) univIdInput.value = '';
        if (fullNameInput) fullNameInput.value = '';
        if (emailInput) emailInput.value = '';
        if (passInput) passInput.value = '';

        closeAuthModal(true);
        showToast(`🎉 Welcome to CampusLib, ${data.fullName}! Your login ID (${data.universityId}) is now active.`, 'success');
        showTab('catalog');

    } catch (err) {
        console.error('Registration network error:', err);
        showToast('Network error during registration. Please try again.', 'error');
    } finally {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = origBtnHtml;
        }
    }
}

// User Login Flow (With Login ID or Email + Password)
async function handleUserLogin(event) {
    if (event) event.preventDefault();

    const identInput = document.getElementById('login-identifier');
    const passInput = document.getElementById('login-password');
    const submitBtn = document.getElementById('btn-submit-login');

    const usernameOrEmail = identInput ? identInput.value.trim() : '';
    const password = passInput ? passInput.value : '';

    if (!usernameOrEmail || !password) {
        showToast('Please enter your Login ID / Email and password', 'error');
        return;
    }

    const origBtnHtml = submitBtn ? submitBtn.innerHTML : '';
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="material-symbols-outlined text-base animate-spin" data-icon="sync">sync</span> Authenticating...';
    }

    try {
        const response = await fetch('/api/v1/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ usernameOrEmail, password })
        });

        const data = await response.json();

        if (!response.ok) {
            const errorMsg = data.message || 'Invalid credentials. Check your Login ID and password.';
            showToast(errorMsg, 'error');
            return;
        }

        // Successfully logged in!
        localStorage.setItem('campus_lib_user', JSON.stringify(data));
        applyAuthenticatedUserProfile(data);

        if (identInput) identInput.value = '';
        if (passInput) passInput.value = '';

        closeAuthModal(true);
        showToast(`Welcome back, ${data.fullName}!`, 'success');

        if (data.role === 'ROLE_LIBRARIAN') {
            showTab('librarian');
        } else if (data.role === 'ROLE_ADMIN') {
            showTab('admin');
        } else {
            showTab('catalog');
        }

    } catch (err) {
        console.error('Login network error:', err);
        showToast('Network error during sign in', 'error');
    } finally {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = origBtnHtml;
        }
    }
}

// Demo persona quick-login
async function loginWithPersona(personaKey) {
    if (personaKey === 'student') {
        // Explicitly clear any custom student profile from localStorage
        // so Demo Access "Aarav Sharma" ALWAYS logs in as Aarav Sharma!
        localStorage.removeItem('campus_lib_student_profile');
    }
    await switchPersona(personaKey, true);
    closeAuthModal(true);
}

// Sign out and revoke active local session
function logoutUser() {
    state.user = null;
    localStorage.removeItem('campus_lib_user');
    localStorage.removeItem('campus_lib_student_profile');

    // Reset Top Navigation Bar & Identity Elements
    const userName = document.getElementById('current-user-name');
    if (userName) userName.textContent = 'Not Signed In';

    const roleBadge = document.getElementById('current-role-badge');
    if (roleBadge) roleBadge.textContent = 'Signed Out';

    const avatarInit = document.getElementById('user-avatar-initial');
    if (avatarInit) avatarInit.textContent = '?';

    const roleIconEl = document.getElementById('top-user-role-icon');
    if (roleIconEl) roleIconEl.textContent = '👤';

    const roleTextEl = document.getElementById('top-user-role-text');
    if (roleTextEl) roleTextEl.textContent = 'Guest';

    // Reset student persona chip back to default demo student
    const chipText = document.getElementById('chip-student-name');
    if (chipText) chipText.textContent = 'Student (Aarav)';

    document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));

    // Clear active badges
    const countBadge = document.getElementById('active-transfers-count');
    if (countBadge) countBadge.textContent = '0';

    // Reset tracker views so unauthenticated state is displayed
    loadStudentTransfers();
    loadStudentLoans();

    showToast('Signed out of CampusLib. Please sign in to continue.', 'info');

    // Navigate to catalog
    showTab('catalog');

    // Open Auth Modal and ask for login immediately!
    openAuthModal(false);
    switchAuthTab('login');
}

// Active Student Profile Persistence & Label Helpers
function getActiveStudentProfile() {
    try {
        const saved = localStorage.getItem('campus_lib_student_profile');
        if (saved) return JSON.parse(saved);
    } catch (e) {}
    return null;
}

function saveActiveStudentProfile(user) {
    if (!user || user.role !== 'ROLE_STUDENT') return;
    localStorage.setItem('campus_lib_student_profile', JSON.stringify(user));
    updateStudentChipLabel(user);
}

function updateStudentChipLabel(user) {
    const chipText = document.getElementById('chip-student-name');
    if (chipText) {
        if (user && user.fullName) {
            const firstName = user.fullName.trim().split(' ')[0];
            chipText.textContent = `Student (${firstName})`;
        } else {
            chipText.textContent = 'Student (Aarav)';
        }
    }
}

// Update Top Navigation Bar & Identity Elements
function applyAuthenticatedUserProfile(user) {
    if (!user) return;
    state.user = user;

    // Update nav role badge
    const roleBadge = document.getElementById('current-role-badge');
    if (roleBadge) {
        roleBadge.textContent = user.universityId ? `${user.role} · ${user.universityId}` : user.role;
    }
    
    // Update display name
    const userName = document.getElementById('current-user-name');
    if (userName) userName.textContent = user.fullName || 'Campus Member';

    // Update initial circle
    const avatarInit = document.getElementById('user-avatar-initial');
    if (avatarInit) {
        avatarInit.textContent = user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U';
    }

    // Update top role pill beside profile name
    const roleIconEl = document.getElementById('top-user-role-icon');
    const roleTextEl = document.getElementById('top-user-role-text');
    if (roleIconEl && roleTextEl) {
        if (user.role === 'ROLE_STUDENT') {
            roleIconEl.textContent = '🎓';
            roleTextEl.textContent = 'Student';
        } else if (user.role === 'ROLE_LIBRARIAN') {
            roleIconEl.textContent = '💻';
            roleTextEl.textContent = 'Librarian';
        } else if (user.role === 'ROLE_ADMIN') {
            roleIconEl.textContent = '🛡️';
            roleTextEl.textContent = 'Admin';
        } else {
            roleIconEl.textContent = '👤';
            roleTextEl.textContent = 'Member';
        }
    }

    // If this user is a student, remember as active student session so user never has to log in again!
    if (user.role === 'ROLE_STUDENT') {
        saveActiveStudentProfile(user);
    } else {
        const savedStudent = getActiveStudentProfile();
        if (savedStudent) {
            updateStudentChipLabel(savedStudent);
        }
    }

    // Update bottom chip active state
    document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
    if (user.role === 'ROLE_STUDENT') {
        const studentChip = document.getElementById('chip-student');
        if (studentChip) studentChip.classList.add('active');
    }

    // Refresh current tab data
    if (state.currentTab === 'tracker') loadStudentData();
    else if (state.currentTab === 'librarian') loadLibrarianData();
    else if (state.currentTab === 'admin') loadAdminData();
}

// ================= AUTHENTICATION & PERSONA SWITCHING =================
async function switchPersona(personaKey, forceDemo = false) {
    if (personaKey === 'student' && !forceDemo) {
        const savedStudent = getActiveStudentProfile();
        if (savedStudent && savedStudent.token) {
            document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
            const activeChip = document.getElementById('chip-student');
            if (activeChip) activeChip.classList.add('active');

            localStorage.setItem('campus_lib_user', JSON.stringify(savedStudent));
            applyAuthenticatedUserProfile(savedStudent);
            showToast(`Active persona: ${savedStudent.fullName} (Student)`, 'info');
            if (state.currentTab === 'librarian' || state.currentTab === 'admin') {
                showTab('catalog');
            } else if (state.currentTab === 'tracker') {
                loadStudentData();
            }
            return;
        }
    }

    const creds = PERSONAS[personaKey];
    if (!creds) return;

    // Update chip styling
    document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
    const activeChip = document.getElementById(`chip-${personaKey.replace('_librarian', '')}`);
    if (activeChip) activeChip.classList.add('active');

    try {
        const response = await fetch('/api/v1/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ usernameOrEmail: creds.email, password: creds.password })
        });

        if (!response.ok) {
            showToast('Login failed for ' + creds.label, 'error');
            return;
        }

        const data = await response.json();
        localStorage.setItem('campus_lib_user', JSON.stringify(data));
        applyAuthenticatedUserProfile(data);

        // Update librarian operating desk branch label & filter dropdown
        const branchInd = document.getElementById('librarian-branch-indicator');
        const branchFilterEl = document.getElementById('librarian-branch-filter');
        if (personaKey === 'cs_librarian') {
            if (branchInd) branchInd.textContent = 'Operating: Computer Science Library (CS-DEPT)';
            state.librarianBranchFilter = '2';
            if (branchFilterEl) branchFilterEl.value = '2';
        } else if (personaKey === 'central_librarian') {
            if (branchInd) branchInd.textContent = 'Operating: Central University Library (CENTRAL)';
            state.librarianBranchFilter = '1';
            if (branchFilterEl) branchFilterEl.value = '1';
        } else {
            if (branchInd) branchInd.textContent = 'Operating: All Campus Facilities';
            state.librarianBranchFilter = '';
            if (branchFilterEl) branchFilterEl.value = '';
        }

        showToast(`Active persona: ${creds.label}`, 'info');

        // Automatically navigate to relevant tab
        if (personaKey === 'cs_librarian' || personaKey === 'central_librarian') {
            showTab('librarian');
        } else if (personaKey === 'admin') {
            showTab('admin');
        } else if (personaKey === 'student' && (state.currentTab === 'librarian' || state.currentTab === 'admin')) {
            showTab('catalog');
        } else {
            // Refresh current tab data
            if (state.currentTab === 'tracker') loadStudentData();
            else if (state.currentTab === 'librarian') loadLibrarianData();
            else if (state.currentTab === 'admin') loadAdminData();
        }

    } catch (e) {
        console.error('Persona switch error:', e);
        showToast('Network error during persona switch', 'error');
    }
}

// API Helper with JWT Bearer Header
async function apiFetch(url, options = {}) {
    const headers = options.headers || {};
    if (state.user && state.user.token) {
        headers['Authorization'] = `Bearer ${state.user.token}`;
    }
    if (!headers['Content-Type'] && options.body && typeof options.body === 'string') {
        headers['Content-Type'] = 'application/json';
    }
    options.headers = headers;
    return fetch(url, options);
}

// ================= TAB NAVIGATION =================
function showTab(tabName) {
    if (!state.user && (tabName === 'tracker' || tabName === 'librarian' || tabName === 'admin')) {
        showToast('Please create your login ID or sign in first', 'info');
        openAuthModal(false);
        return;
    }

    state.currentTab = tabName;
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

    const activeBtn = document.getElementById(`tab-btn-${tabName}`);
    if (activeBtn) activeBtn.classList.add('active');
    
    const activeContent = document.getElementById(`tab-${tabName}`);
    if (activeContent) activeContent.classList.add('active');

    if (tabName === 'tracker') loadStudentData();
    else if (tabName === 'librarian') loadLibrarianData();
    else if (tabName === 'admin') loadAdminData();
    else if (tabName === 'catalog') {
        const input = document.getElementById('catalog-search-input');
        loadCatalog(input ? input.value : '');
    }
}

function showSubTab(subTabName) {
    state.currentSubTab = subTabName;
    document.querySelectorAll('.sub-tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.subtab-content').forEach(c => c.classList.remove('active'));

    const activeBtn = document.getElementById(`subtab-btn-${subTabName}`);
    if (activeBtn) activeBtn.classList.add('active');
    
    const activeContent = document.getElementById(`subtab-${subTabName}`);
    if (activeContent) activeContent.classList.add('active');

    loadLibrarianData();
}

// ================= PAGE 1: CATALOG & REAL-TIME STOCK MATRIX =================
async function loadBranches() {
    try {
        const res = await apiFetch('/api/v1/branches');
        if (res.ok) {
            state.branches = await res.json();
            const destSelect = document.getElementById('modal-destination-select');
            if (destSelect) {
                destSelect.innerHTML = state.branches.map(b => `<option value="${b.id}">${b.name} (${b.code})</option>`).join('');
            }
        }
    } catch (e) {
        console.error('Failed to load branches:', e);
    }
}

function handleSearchInput(e) {
    clearTimeout(state.searchDebounceTimer);
    const query = e.target.value.trim();
    state.searchDebounceTimer = setTimeout(() => {
        loadCatalog(query);
    }, 250);
}

function clearSearch() {
    const input = document.getElementById('catalog-search-input');
    if (input) input.value = '';
    loadCatalog('');
}

function quickSearch(term) {
    const input = document.getElementById('catalog-search-input');
    if (input) input.value = term;
    loadCatalog(term);
}

// ================= CATALOG BRANCH & AVAILABILITY FILTERS =================
function setCatalogBranchFilter(branchCode) {
    state.catalogBranchFilter = branchCode;
    document.querySelectorAll('.branch-pill').forEach(btn => btn.classList.remove('active'));
    const activeBtn = document.getElementById(`branch-filter-${branchCode}`);
    if (activeBtn) activeBtn.classList.add('active');
    renderCatalogCards();
}

function toggleAvailableOnlyFilter() {
    state.catalogAvailableOnly = !state.catalogAvailableOnly;
    const btn = document.getElementById('filter-available-toggle');
    const icon = document.getElementById('avail-toggle-icon');
    if (state.catalogAvailableOnly) {
        if (btn) {
            btn.classList.add('bg-powder-sage', 'text-primary', 'border-sage-mist');
            btn.classList.remove('bg-surface-container-lowest');
        }
        if (icon) {
            icon.textContent = 'check_box';
            icon.classList.add('text-primary');
        }
    } else {
        if (btn) {
            btn.classList.remove('bg-powder-sage', 'text-primary', 'border-sage-mist');
            btn.classList.add('bg-surface-container-lowest');
        }
        if (icon) {
            icon.textContent = 'check_box_outline_blank';
            icon.classList.remove('text-primary');
        }
    }
    renderCatalogCards();
}

function renderInventoryChipsHtml(bookId) {
    const inv = state.cachedInventory[bookId];
    if (!inv) return '<div class="text-[11px] text-[#78614c] italic">Checking physical shelves...</div>';

    return inv.branchBreakdown.map(bb => {
        if (bb.available > 0) {
            return `
                <div class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-[#dbe8d8]/90 text-[#1e4620] text-[11px] font-semibold border border-[#91b88e]/60 mr-1.5 mb-1 shadow-xs">
                    <span class="w-1.5 h-1.5 rounded-full bg-[#2e6b31]"></span>
                    <span><strong>${bb.branchCode}:</strong> ${bb.available} on shelf</span>
                </div>
            `;
        } else if (bb.reserved > 0 || bb.inTransit > 0) {
            return `
                <div class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-[#faecd8]/90 text-[#7a4515] text-[11px] font-semibold border border-[#d6af83]/60 mr-1.5 mb-1 shadow-xs">
                    <span class="w-1.5 h-1.5 rounded-full bg-[#a35e20]"></span>
                    <span>${bb.branchCode}: In Transit / Hold</span>
                </div>
            `;
        } else {
            return `
                <div class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-[#ede5db]/80 text-[#695847] text-[11px] font-medium border border-[#bda996]/50 mr-1.5 mb-1">
                    <span class="w-1.5 h-1.5 rounded-full bg-[#8c7866]"></span>
                    <span>${bb.branchCode}: 0 Available</span>
                </div>
            `;
        }
    }).join('');
}

function renderCatalogCards() {
    const grid = document.getElementById('books-grid');
    const countText = document.getElementById('results-count-text');
    if (!grid) return;

    let filtered = state.catalog || [];

    // Filter by branch holdings
    if (state.catalogBranchFilter !== 'ALL') {
        filtered = filtered.filter(b => {
            const inv = state.cachedInventory[b.id];
            if (!inv) return true; // keep visible while holdings load
            return inv.branchBreakdown.some(bb => bb.branchCode === state.catalogBranchFilter && (bb.available > 0 || bb.reserved > 0 || bb.inTransit > 0));
        });
    }

    // Filter by available on shelf
    if (state.catalogAvailableOnly) {
        filtered = filtered.filter(b => {
            const inv = state.cachedInventory[b.id];
            if (!inv) return true;
            if (state.catalogBranchFilter === 'ALL') {
                return inv.totalAvailable > 0;
            } else {
                return inv.branchBreakdown.some(bb => bb.branchCode === state.catalogBranchFilter && bb.available > 0);
            }
        });
    }

    const branchNameMap = {
        'ALL': 'all facilities',
        'CENTRAL': 'Central Library',
        'CS-DEPT': 'Turing CS Stacks',
        'MECH-DEPT': 'Franklin Mech Hall'
    };

    if (countText) {
        const filterSuffix = state.catalogBranchFilter !== 'ALL' ? ` (at ${branchNameMap[state.catalogBranchFilter]})` : '';
        const availSuffix = state.catalogAvailableOnly ? ' [in stock]' : '';
        countText.textContent = `Displaying ${filtered.length} academic title${filtered.length === 1 ? '' : 's'}${filterSuffix}${availSuffix}`;
    }

    if (filtered.length === 0) {
        grid.innerHTML = `
            <div class="open-book-spread relative w-full rounded-3xl p-12 text-center border-2 border-[#2b1c11] shadow-2xl">
                <div class="max-w-md mx-auto py-8 space-y-3">
                    <span class="material-symbols-outlined text-4xl text-[#5c4733]" data-icon="menu_book">menu_book</span>
                    <h3 class="text-xl font-headline font-bold text-[#22160d]">No Volumes Matching Current Criteria</h3>
                    <p class="text-xs text-[#5c4733] font-headline italic">Try switching to "All 3 Campus Libraries" or toggling "In Stock Only" off to search our archival stacks.</p>
                </div>
            </div>
        `;
        return;
    }

    // Group filtered books into pairs for open 2-page book spreads
    const spreads = [];
    for (let i = 0; i < filtered.length; i += 2) {
        spreads.push({
            left: filtered[i],
            leftIdx: i,
            right: filtered[i + 1] || null,
            rightIdx: i + 1,
            spreadNum: Math.floor(i / 2) + 1,
            pageLeftNum: i + 1,
            pageRightNum: i + 2
        });
    }

    grid.innerHTML = spreads.map(spread => `
        <div class="open-book-spread relative w-full rounded-2xl sm:rounded-3xl shadow-[0_24px_56px_-12px_rgba(0,0,0,0.8)] border-2 border-[#2b1b11] mb-10 overflow-hidden">
            <!-- Central Spine Binding Crease Shading -->
            <div class="absolute top-0 bottom-0 left-[49.3%] w-[1.4%] bg-gradient-to-r from-black/35 via-black/10 to-transparent pointer-events-none z-10 hidden md:block"></div>
            
            <!-- Two-Page Content Grid Aligned with Left & Right Pages of Livro Base -->
            <div class="relative z-10 grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-12 lg:gap-16 p-4 sm:p-7 md:p-9 lg:p-11 min-h-[440px]">
                
                <!-- LEFT PAGE -->
                <div class="book-page book-page-left flex flex-col justify-between">
                    ${renderSingleBookPageHtml(spread.left, spread.leftIdx, spread.pageLeftNum)}
                </div>

                <!-- RIGHT PAGE -->
                <div class="book-page book-page-right flex flex-col justify-between border-t md:border-t-0 border-[#7c634c]/20 pt-6 md:pt-0">
                    ${spread.right 
                        ? renderSingleBookPageHtml(spread.right, spread.rightIdx, spread.pageRightNum) 
                        : renderExLibrisFolioHtml(spread.pageRightNum)}
                </div>

            </div>
        </div>
    `).join('');
}

function renderSingleBookPageHtml(b, idx, pageNum) {
    const theme = COVER_THEMES[idx % COVER_THEMES.length];
    return `
        <article class="flex flex-col justify-between h-full space-y-4">
            
            <!-- Folio Page Header / Stamp -->
            <div class="flex items-center justify-between border-b border-[#7c634c]/30 pb-2 text-[10px] sm:text-[11px] font-headline text-[#5c4733]">
                <span class="tracking-widest uppercase font-semibold">Athenaeum Registry · Folio ${pageNum}</span>
                <span class="font-mono text-[#7a6450] text-[10px]">ISBN: ${b.isbn}</span>
            </div>

            <!-- Book Body (Archival Bookplate + Title & Metadata) -->
            <div class="flex flex-col sm:flex-row gap-4 sm:gap-5 flex-grow items-start">
                
                <!-- Archival Bookplate Thumbnail -->
                <div class="w-28 sm:w-32 h-44 rounded-lg bg-[#f5ecdf]/85 p-3 flex flex-col justify-between shrink-0 border border-[#8f755b]/40 shadow-md relative overflow-hidden group">
                    <div class="text-[9px] uppercase tracking-wider text-[#634e3a] font-bold">${theme.series}</div>
                    <div class="my-auto text-center">
                        <span class="material-symbols-outlined text-[#634e3a] text-3xl block mb-1" data-icon="${theme.icon}">${theme.icon}</span>
                        <p class="font-headline text-[11px] font-bold text-[#2a1d12] leading-tight px-1 line-clamp-2">${escapeHtml(b.title)}</p>
                    </div>
                    <div class="text-[9px] text-[#78614a] border-t border-[#8f755b]/20 pt-1 font-mono">${b.edition || 'Academic Ed.'}</div>
                </div>

                <!-- Title, Author, and Campus Shelf Stock -->
                <div class="flex-grow flex flex-col justify-between w-full">
                    <div>
                        <div class="flex items-center gap-2 mb-1.5">
                            <span class="text-[10px] font-bold text-[#3d2f21] bg-[#e7d8c5]/90 px-2.5 py-0.5 rounded-full border border-[#bfa891]/60">
                                ${b.edition || 'Standard Ed.'}
                            </span>
                        </div>
                        <h3 class="text-lg sm:text-xl md:text-2xl font-headline font-bold text-[#22160d] leading-snug tracking-tight">
                            ${escapeHtml(b.title)}
                        </h3>
                        <p class="text-xs sm:text-sm text-[#4f3c2b] italic mt-0.5 mb-3 font-headline">
                            by ${escapeHtml(b.author)}
                        </p>
                    </div>

                    <!-- Campus Branch Stock Chips -->
                    <div class="space-y-1.5 mb-1" id="inv-chips-${b.id}">
                        ${renderInventoryChipsHtml(b.id)}
                    </div>
                </div>
            </div>

            <!-- Page Bottom Actions & Transit Info -->
            <div class="border-t border-[#7c634c]/30 pt-3 flex items-center justify-between flex-wrap gap-2 text-xs">
                <span class="text-[#5c4733] flex items-center gap-1 font-headline italic text-[11px]">
                    <span class="material-symbols-outlined text-sm" data-icon="electric_bolt">electric_bolt</span> Campus Transit: 15–20 min
                </span>
                <button onclick="openTransferModal(${b.id})" class="px-4 py-2 rounded-full bg-[#34261a] hover:bg-[#20160d] text-[#f7f2ea] text-xs font-semibold active:scale-[0.98] transition-all flex items-center gap-1.5 shadow-md border border-[#5d4734]">
                    <span class="material-symbols-outlined text-sm" data-icon="near_me">near_me</span>
                    Request Branch Courier
                </button>
            </div>

        </article>
    `;
}

function renderExLibrisFolioHtml(pageNum) {
    return `
        <div class="flex flex-col justify-between h-full p-4 text-center border-2 border-dashed border-[#8c745d]/40 rounded-2xl bg-[#efe5d6]/30">
            <div class="text-[10px] sm:text-[11px] font-headline text-[#5c4733] tracking-widest uppercase border-b border-[#7c634c]/20 pb-2">
                Athenaeum Exchange · Folio ${pageNum}
            </div>
            <div class="my-auto py-6 space-y-3">
                <div class="w-14 h-14 mx-auto rounded-full bg-[#dfceb9] border border-[#8c745d]/50 flex items-center justify-center text-[#34261a] text-2xl shadow-inner">
                    🏛️
                </div>
                <h4 class="font-headline font-bold text-lg text-[#261a10]">Inter-Branch Archival Registry</h4>
                <p class="text-xs text-[#523e2c] max-w-xs mx-auto italic font-headline leading-relaxed">
                    “A library is not a luxury but one of the necessities of life.”
                </p>
                <div class="text-[11px] text-[#6b533f]">
                    Select any volume across Central, CS, or Mech stacks for automated electric courier delivery.
                </div>
            </div>
            <div class="border-t border-[#7c634c]/20 pt-2 text-[10px] text-[#7a6450] tracking-wider uppercase font-mono">
                Campus Inter-Branch Delivery Network
            </div>
        </div>
    `;
}

async function loadCatalog(query) {
    const grid = document.getElementById('books-grid');
    const countText = document.getElementById('results-count-text');
    if (countText) countText.textContent = 'Searching campus holdings...';

    try {
        const url = query ? `/api/v1/books?query=${encodeURIComponent(query)}&size=20` : '/api/v1/books?size=20';
        const res = await apiFetch(url);
        if (!res.ok) throw new Error('Failed to fetch books');

        const pageData = await res.json();
        const books = pageData.content || [];
        state.catalog = books;

        renderCatalogCards();

        // Fetch detailed inventory matrix for each card asynchronously
        for (const book of books) {
            fetchBookInventory(book.id);
        }

    } catch (e) {
        console.error('Catalog error:', e);
        if (grid) grid.innerHTML = `<div class="col-span-full text-center py-10 text-error">Failed to load academic titles from server.</div>`;
    }
}

async function fetchBookInventory(bookId) {
    try {
        const res = await apiFetch(`/api/v1/books/${bookId}/inventory`);
        if (!res.ok) return;

        const inv = await res.json();
        state.cachedInventory[bookId] = inv;

        const chipsContainer = document.getElementById(`inv-chips-${bookId}`);
        if (chipsContainer) {
            chipsContainer.innerHTML = renderInventoryChipsHtml(bookId);
        }

        // Re-evaluate filters if active
        if (state.catalogBranchFilter !== 'ALL' || state.catalogAvailableOnly) {
            renderCatalogCards();
        }

    } catch (e) {
        console.error('Inventory fetch error:', e);
    }
}

// ================= TRANSFER MODAL & PESSIMISTIC LOCKING SUBMISSION =================
async function openTransferModal(bookId) {
    if (!state.user) {
        showToast('Please create your login ID or sign in before requesting book transfers', 'warning');
        openAuthModal(false);
        return;
    }

    const book = state.catalog.find(b => b.id === bookId);
    if (!book) return;

    state.selectedBookForTransfer = book;
    const titleEl = document.getElementById('modal-book-title');
    if (titleEl) titleEl.textContent = book.title;

    const detailsEl = document.getElementById('modal-book-details');
    if (detailsEl) detailsEl.textContent = `by ${book.author} · ISBN: ${book.isbn}`;

    const copySelect = document.getElementById('modal-copy-select');
    copySelect.innerHTML = '<option value="">Checking physical stack copies...</option>';

    const modal = document.getElementById('transfer-modal');
    modal.classList.add('open');

    // Get live inventory
    try {
        const res = await apiFetch(`/api/v1/books/${bookId}/inventory`);
        if (!res.ok) throw new Error('Inventory load failed');
        const inv = await res.json();
        state.cachedInventory[bookId] = inv;

        const availableCopies = (inv.availableCopies || []).filter(c => c.status === 'AVAILABLE');

        if (availableCopies.length === 0) {
            copySelect.innerHTML = '<option value="" disabled>No physical copies currently available on campus stacks</option>';
            document.getElementById('btn-confirm-transfer').disabled = true;
            document.getElementById('modal-source-shelf-hint').textContent = 'All volumes are currently checked out or in transit.';
            return;
        }

        document.getElementById('btn-confirm-transfer').disabled = false;
        copySelect.innerHTML = availableCopies.map(c => `
            <option value="${c.copyId}" data-branch="${c.branchCode}" data-shelf="${c.shelfLocation}">
                ${c.branchCode} Library — Barcode [${c.barcode}] (${c.shelfLocation || 'Stack Shelf'})
            </option>
        `).join('');

        updateTransferSourceInfo();

    } catch (e) {
        console.error('Modal inventory error:', e);
        copySelect.innerHTML = '<option value="" disabled>Error reading shelf holdings</option>';
    }
}

function updateTransferSourceInfo() {
    const copySelect = document.getElementById('modal-copy-select');
    const selectedOption = copySelect.options[copySelect.selectedIndex];
    const hint = document.getElementById('modal-source-shelf-hint');
    if (selectedOption && selectedOption.dataset.shelf && hint) {
        hint.innerHTML = `<span class="material-symbols-outlined text-xs" data-icon="pin_drop">pin_drop</span> Pull Location: <strong>${selectedOption.dataset.branch}</strong> (${selectedOption.dataset.shelf})`;
    }
}

function closeTransferModal() {
    const modal = document.getElementById('transfer-modal');
    if (modal) modal.classList.remove('open');
    state.selectedBookForTransfer = null;
}

async function submitTransferRequest() {
    const copyId = document.getElementById('modal-copy-select').value;
    const destBranchId = document.getElementById('modal-destination-select').value;

    if (!copyId) {
        showToast('Please select an available physical book copy', 'error');
        return;
    }

    const btn = document.getElementById('btn-confirm-transfer');
    btn.disabled = true;
    btn.innerHTML = '🔒 Securing Pessimistic Lock...';

    try {
        const res = await apiFetch('/api/v1/transfers/request', {
            method: 'POST',
            body: JSON.stringify({
                copyId: parseInt(copyId),
                destinationBranchId: parseInt(destBranchId)
            })
        });

        if (res.status === 409) {
            const errorData = await res.json();
            showToast(`⚠️ Lock Conflict: ${errorData.message || 'Copy was just reserved by another student!'}`, 'error');
            btn.disabled = false;
            btn.innerHTML = '<span class="material-symbols-outlined text-sm" data-icon="near_me">near_me</span> Confirm Transfer';
            return;
        }

        if (!res.ok) {
            const err = await res.json();
            showToast(`Error: ${err.message || 'Transfer request failed'}`, 'error');
            btn.disabled = false;
            btn.innerHTML = '<span class="material-symbols-outlined text-sm" data-icon="near_me">near_me</span> Confirm Transfer';
            return;
        }

        const transfer = await res.json();
        showToast(`✅ Transfer #${transfer.id} confirmed! Reserved via database lock.`, 'success');
        closeTransferModal();

        // Refresh inventory and student delivery pipeline
        if (state.selectedBookForTransfer) {
            fetchBookInventory(state.selectedBookForTransfer.id);
        }
        loadStudentData();

    } catch (e) {
        console.error('Transfer submission error:', e);
        showToast('Network error during transfer submission', 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<span class="material-symbols-outlined text-sm" data-icon="near_me">near_me</span> Confirm Transfer';
    }
}

// ================= PAGE 2: STUDENT TRACKER (TRANSFERS & LOANS) =================
async function loadStudentData() {
    await loadStudentTransfers();
    await loadStudentLoans();
}

async function loadStudentTransfers() {
    const container = document.getElementById('transfers-list');
    if (!container) return;

    if (!state.user || !state.user.token) {
        container.innerHTML = `
            <div class="text-center py-10 px-4 bg-surface-container-low/70 rounded-2xl border border-outline-variant/30 space-y-3">
                <div class="w-12 h-12 rounded-full bg-powder-sage/60 text-primary mx-auto flex items-center justify-center shadow-sm">
                    <span class="material-symbols-outlined text-2xl" data-icon="lock">lock</span>
                </div>
                <h4 class="text-base font-headline font-semibold text-on-surface">Student Sign-In Required</h4>
                <p class="text-xs text-on-surface-variant max-w-md mx-auto">
                    Please sign in with your student account to view your active book requests, real-time courier transit, and 48-hour pickup holds.
                </p>
                <div class="pt-2 flex items-center justify-center gap-3 flex-wrap">
                    <button onclick="switchPersona('student')" class="px-5 py-2.5 rounded-full bg-primary text-on-primary text-xs font-semibold hover:opacity-90 transition-all shadow-md flex items-center gap-1.5">
                        <span class="material-symbols-outlined text-sm" data-icon="school">school</span>
                        Sign In as Student
                    </button>
                    <button onclick="openAuthModal(false)" class="px-4 py-2.5 rounded-full bg-surface-container border border-outline-variant/40 text-xs font-medium text-on-surface hover:bg-surface-container-high transition-colors">
                        Custom Login / Register
                    </button>
                </div>
            </div>
        `;
        return;
    }

    container.innerHTML = `
        <div class="text-xs text-outline py-8 text-center flex items-center justify-center gap-2">
            <span class="material-symbols-outlined text-base animate-spin" data-icon="sync">sync</span>
            Reading active courier pipeline...
        </div>
    `;

    try {
        const res = await apiFetch('/api/v1/transfers/my-requests');

        if (res.status === 401 || res.status === 403) {
            container.innerHTML = `
                <div class="text-center py-10 px-4 bg-surface-container-low/70 rounded-2xl border border-outline-variant/30 space-y-3">
                    <div class="w-12 h-12 rounded-full bg-cream-vellum text-tertiary mx-auto flex items-center justify-center shadow-sm">
                        <span class="material-symbols-outlined text-2xl" data-icon="manage_accounts">manage_accounts</span>
                    </div>
                    <h4 class="text-base font-headline font-semibold text-on-surface">Student Account Required</h4>
                    <p class="text-xs text-on-surface-variant max-w-md mx-auto">
                        You are currently signed in as <strong>${escapeHtml(state.user.role || 'Staff')}</strong>. Student transit orders are tied to student borrower accounts.
                    </p>
                    <div class="pt-2">
                        <button onclick="switchPersona('student')" class="px-5 py-2.5 rounded-full bg-primary text-on-primary text-xs font-semibold hover:opacity-90 transition-all shadow-md">
                            Switch to Student Persona
                        </button>
                    </div>
                </div>
            `;
            return;
        }

        if (!res.ok) {
            container.innerHTML = `
                <div class="text-center py-8 px-4 bg-surface-container-low/70 rounded-2xl border border-outline-variant/30 space-y-2">
                    <p class="text-xs text-error font-medium">Unable to load transfer orders (${res.status})</p>
                    <button onclick="loadStudentTransfers()" class="px-4 py-1.5 rounded-full bg-surface-container text-xs hover:bg-surface-container-high transition-colors">
                        Try Again
                    </button>
                </div>
            `;
            return;
        }

        const transfers = await res.json();
        const activeCount = transfers.filter(t => t.status !== 'COMPLETED' && t.status !== 'EXPIRED').length;
        
        const countBadge = document.getElementById('active-transfers-count');
        if (countBadge) countBadge.textContent = activeCount;

        if (transfers.length === 0) {
            container.innerHTML = `
                <div class="text-center py-12 bg-surface-container-low/50 rounded-2xl border border-outline-variant/30">
                    <span class="material-symbols-outlined text-3xl text-outline mb-1" data-icon="local_shipping">local_shipping</span>
                    <p class="text-sm font-medium text-on-surface">No active campus courier transfers</p>
                    <p class="text-xs text-outline mt-1">Search the catalog and request an inter-branch transfer to track delivery in real time.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = transfers.map(t => {
            // Determine active step index: 0=REQUESTED, 1=DISPATCHED, 2=READY_FOR_PICKUP, 3=COMPLETED
            let stepIndex = 0;
            if (t.status === 'DISPATCHED') stepIndex = 1;
            else if (t.status === 'READY_FOR_PICKUP') stepIndex = 2;
            else if (t.status === 'COMPLETED') stepIndex = 3;

            let holdNoticeHtml = '';
            if (t.status === 'READY_FOR_PICKUP') {
                holdNoticeHtml = `
                    <div class="mt-4 p-4 rounded-xl bg-tertiary-fixed/30 border border-tertiary-fixed/60 flex items-center justify-between flex-wrap gap-3">
                        <div class="flex items-center gap-3">
                            <span class="material-symbols-outlined text-tertiary text-2xl" data-icon="timer">timer</span>
                            <div>
                                <div class="text-xs font-bold text-tertiary uppercase tracking-wide">Ready for Pickup at ${t.toBranchCode} Desk!</div>
                                <div class="text-xs text-on-surface">48-Hour pickup hold active. Present barcode at counter to collect.</div>
                                <div class="text-xs font-semibold text-tertiary mt-1 hold-timer" data-expires="${t.expiresAt || ''}">
                                    ⏱️ 48h Hold Window Active
                                </div>
                            </div>
                        </div>
                        <span class="px-3 py-1 rounded-full bg-surface-container-lowest text-xs font-mono font-bold text-on-surface border border-outline-variant/40">
                            Hold ID: #${t.id}
                        </span>
                    </div>
                `;
            }

            return `
                <div class="bg-surface-container-lowest rounded-2xl p-6 border border-outline-variant/30 shadow-[0_2px_12px_-2px_rgba(101,113,102,0.06)] space-y-4">
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                        <div>
                            <div class="text-lg font-headline font-semibold text-on-surface">${escapeHtml(t.bookTitle)}</div>
                            <div class="text-xs text-on-surface-variant flex items-center gap-2 mt-0.5">
                                <span class="font-medium text-primary">${t.fromBranchCode}</span>
                                <span>➔</span>
                                <span class="font-medium text-primary">${t.toBranchCode} Desk</span>
                                <span class="text-outline font-mono">[Barcode: ${t.barcode}]</span>
                            </div>
                        </div>
                        <span class="px-3 py-1 rounded-full text-xs font-semibold bg-powder-sage text-primary self-start sm:self-auto">
                            ${t.status}
                        </span>
                    </div>

                    <!-- Horizontal Stepper -->
                    <div class="progress-stepper">
                        <div class="step-node ${stepIndex >= 0 ? (stepIndex === 0 ? 'active' : 'completed') : ''}">
                            <div class="step-dot">${stepIndex > 0 ? '✓' : '1'}</div>
                            <span class="step-label">1. Requested</span>
                        </div>
                        <div class="step-node ${stepIndex >= 1 ? (stepIndex === 1 ? 'active' : 'completed') : ''}">
                            <div class="step-dot">${stepIndex > 1 ? '✓' : '2'}</div>
                            <span class="step-label">2. In Transit</span>
                        </div>
                        <div class="step-node ${stepIndex >= 2 ? (stepIndex === 2 ? 'active' : 'completed') : ''}">
                            <div class="step-dot">${stepIndex > 2 ? '✓' : '3'}</div>
                            <span class="step-label">3. Ready at Desk</span>
                        </div>
                        <div class="step-node ${stepIndex >= 3 ? 'completed' : ''}">
                            <div class="step-dot">${stepIndex >= 3 ? '✓' : '4'}</div>
                            <span class="step-label">4. Collected</span>
                        </div>
                    </div>

                    ${holdNoticeHtml}
                </div>
            `;
        }).join('');

        tickHoldTimers();

    } catch (e) {
        console.error('Student transfers load error:', e);
        container.innerHTML = `
            <div class="text-center py-8 px-4 bg-surface-container-low/70 rounded-2xl border border-outline-variant/30 space-y-2">
                <p class="text-xs text-error font-medium">Network error while connecting to transfer service.</p>
                <button onclick="loadStudentTransfers()" class="px-4 py-1.5 rounded-full bg-surface-container text-xs hover:bg-surface-container-high transition-colors">
                    Try Again
                </button>
            </div>
        `;
    }
}

async function loadStudentLoans() {
    const container = document.getElementById('loans-list');
    if (!container) return;

    if (!state.user || !state.user.token) {
        container.innerHTML = `
            <div class="col-span-full text-center py-8 px-4 bg-surface-container-low/70 rounded-2xl border border-outline-variant/30">
                <span class="material-symbols-outlined text-2xl text-outline mb-1" data-icon="menu_book">menu_book</span>
                <p class="text-xs text-on-surface-variant font-medium">Sign in as a student to view your borrowed circulation records.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = `
        <div class="col-span-full text-center py-6 text-xs text-outline flex items-center justify-center gap-2">
            <span class="material-symbols-outlined text-base animate-spin" data-icon="sync">sync</span>
            Loading active loans...
        </div>
    `;

    try {
        const res = await apiFetch('/api/v1/loans/my-loans');
        if (res.status === 401 || res.status === 403) {
            container.innerHTML = `
                <div class="col-span-full text-center py-6 bg-surface-container-low/70 rounded-2xl border border-outline-variant/30 text-xs text-outline">
                    Sign in with a student account to see your circulation history.
                </div>
            `;
            return;
        }

        if (!res.ok) {
            container.innerHTML = `
                <div class="col-span-full text-center py-6 bg-surface-container-low/70 rounded-2xl border border-outline-variant/30 text-xs text-error">
                    Failed to load loans. <button onclick="loadStudentLoans()" class="underline ml-1">Retry</button>
                </div>
            `;
            return;
        }

        const loans = await res.json();
        if (loans.length === 0) {
            container.innerHTML = `
                <div class="col-span-full text-center py-8 bg-surface-container-low/50 rounded-2xl border border-outline-variant/30">
                    <span class="material-symbols-outlined text-2xl text-outline mb-1" data-icon="auto_stories">auto_stories</span>
                    <p class="text-xs text-on-surface-variant font-medium">No borrowed books currently on your account</p>
                </div>
            `;
            return;
        }

        container.innerHTML = loans.map(l => {
            const isReturned = l.returnDate !== null;
            return `
                <div class="bg-surface-container-lowest rounded-2xl p-5 border border-outline-variant/30 shadow-sm flex flex-col justify-between space-y-3">
                    <div>
                        <div class="flex items-start justify-between gap-2">
                            <span class="font-headline font-semibold text-on-surface text-base">${escapeHtml(l.bookTitle)}</span>
                            ${l.fineAmount > 0 ? `<span class="px-2.5 py-0.5 rounded-full bg-error text-white text-[11px] font-bold">Fine: ₹${l.fineAmount.toFixed(2)}</span>` : ''}
                        </div>
                        <div class="text-xs text-outline font-mono mt-1">Barcode: ${l.barcode}</div>
                    </div>
                    <div class="border-t border-outline-variant/20 pt-3 flex items-center justify-between text-xs">
                        <div>
                            Due: <strong class="${l.isOverdue ? 'text-error' : 'text-on-surface'}">${l.dueDate}</strong>
                            ${l.isOverdue ? '<span class="text-error font-bold ml-1">(OVERDUE)</span>' : ''}
                        </div>
                        <div>
                            ${isReturned ? '<span class="text-outline">Returned</span>' : '<span class="text-success font-semibold">Active Loan</span>'}
                        </div>
                    </div>
                </div>
            `;
        }).join('');

    } catch (e) {
        console.error('Student loans load error:', e);
        container.innerHTML = `
            <div class="col-span-full text-center py-6 bg-surface-container-low/70 rounded-2xl border border-outline-variant/30 text-xs text-error">
                Failed to load loans. <button onclick="loadStudentLoans()" class="underline ml-1">Retry</button>
            </div>
        `;
    }
}

// ================= PAGE 3: LIBRARIAN OPERATIONS DESK =================
// ================= PAGE 3: LIBRARIAN OPERATIONS DESK =================
function handleLibrarianBranchFilterChange() {
    const sel = document.getElementById('librarian-branch-filter');
    state.librarianBranchFilter = sel ? sel.value : '';
    loadLibrarianData();
}

async function loadLibrarianData() {
    if (state.currentSubTab === 'dispatch') loadDispatchQueue();
    else if (state.currentSubTab === 'receive') loadReceiveQueue();
    else if (state.currentSubTab === 'return') {
        loadHoldShelfQueue();
        loadActiveLoansForReturn();
        loadStudentsForCheckout();
    }
}

async function loadDispatchQueue() {
    const container = document.getElementById('dispatch-queue-container');
    if (!container) return;
    container.innerHTML = '<div class="text-xs text-outline py-6 text-center">Reading pending outbound requests...</div>';

    try {
        let url = '/api/v1/transfers/queue?status=REQUESTED';
        if (state.librarianBranchFilter) url += `&branchId=${state.librarianBranchFilter}`;

        const res = await apiFetch(url);
        if (!res.ok) return;
        const queue = await res.json();

        if (queue.length === 0) {
            container.innerHTML = `
                <div class="text-center py-12 bg-surface-container-lowest rounded-2xl border border-outline-variant/30">
                    <span class="material-symbols-outlined text-3xl text-outline mb-1">done_all</span>
                    <p class="text-sm font-medium text-on-surface">Dispatch Queue is Clear</p>
                    <p class="text-xs text-outline mt-1">All requested branch volumes have been pulled and dispatched to transit.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = `
            <table class="op-table">
                <thead>
                    <tr>
                        <th>Transfer #</th>
                        <th>Book Volume</th>
                        <th>Barcode</th>
                        <th>Stack Location</th>
                        <th>Destination</th>
                        <th>Student</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    ${queue.map(t => `
                        <tr>
                            <td class="font-mono font-semibold text-outline">#${t.id}</td>
                            <td><strong class="font-headline text-sm">${escapeHtml(t.bookTitle)}</strong></td>
                            <td><span class="font-mono text-xs text-outline">${t.barcode}</span></td>
                            <td>
                                <span class="px-2.5 py-1 rounded-full bg-powder-sage text-primary text-xs font-semibold">
                                    ${t.fromBranchCode}
                                </span>
                            </td>
                            <td><strong class="text-on-surface">${t.toBranchCode} Desk</strong></td>
                            <td class="text-xs">${escapeHtml(t.studentName)} (${t.universityId})</td>
                            <td>
                                <button class="px-3.5 py-1.5 rounded-full bg-primary-container text-on-primary-container text-xs font-semibold hover:opacity-95 transition-all shadow-sm flex items-center gap-1" onclick="dispatchParcel(${t.id})">
                                    <span class="material-symbols-outlined text-xs" data-icon="local_shipping">local_shipping</span>
                                    Dispatch
                                </button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    } catch (e) {
        console.error('Dispatch queue error:', e);
    }
}

async function dispatchParcel(transferId) {
    try {
        const res = await apiFetch(`/api/v1/transfers/${transferId}/dispatch`, { method: 'PUT' });
        if (!res.ok) {
            const err = await res.json();
            showToast(`Error: ${err.message}`, 'error');
            return;
        }
        showToast(`Transfer #${transferId} marked IN_TRANSIT! Dispatched to courier.`, 'success');
        loadDispatchQueue();
        loadStudentTransfers();
    } catch (e) {
        showToast('Network error during dispatch', 'error');
    }
}

async function loadReceiveQueue() {
    const container = document.getElementById('receive-queue-container');
    if (!container) return;
    container.innerHTML = '<div class="text-xs text-outline py-6 text-center">Checking incoming transit parcels...</div>';

    try {
        let url = '/api/v1/transfers/queue?status=DISPATCHED';
        if (state.librarianBranchFilter) url += `&branchId=${state.librarianBranchFilter}`;

        const res = await apiFetch(url);
        if (!res.ok) return;
        const queue = await res.json();

        if (queue.length === 0) {
            container.innerHTML = `
                <div class="text-center py-12 bg-surface-container-lowest rounded-2xl border border-outline-variant/30">
                    <span class="material-symbols-outlined text-3xl text-outline mb-1">markunread_mailbox</span>
                    <p class="text-sm font-medium text-on-surface">No Inbound Courier Parcels</p>
                    <p class="text-xs text-outline mt-1">No cross-branch parcels currently en route to your counter.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = `
            <table class="op-table">
                <thead>
                    <tr>
                        <th>Transfer #</th>
                        <th>Book Volume</th>
                        <th>Barcode</th>
                        <th>Origin Branch</th>
                        <th>Destination Desk</th>
                        <th>Patron</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    ${queue.map(t => `
                        <tr>
                            <td class="font-mono font-semibold text-outline">#${t.id}</td>
                            <td><strong class="font-headline text-sm">${escapeHtml(t.bookTitle)}</strong></td>
                            <td><span class="font-mono text-xs text-outline">${t.barcode}</span></td>
                            <td>${t.fromBranchCode}</td>
                            <td><span class="px-2.5 py-1 rounded-full bg-secondary-fixed text-on-secondary-fixed text-xs font-semibold">${t.toBranchCode}</span></td>
                            <td class="text-xs">${escapeHtml(t.studentName)} (${t.universityId})</td>
                            <td>
                                <button class="px-3.5 py-1.5 rounded-full bg-secondary text-white text-xs font-semibold hover:opacity-95 transition-all shadow-sm flex items-center gap-1" onclick="receiveParcel(${t.id})">
                                    <span class="material-symbols-outlined text-xs" data-icon="qr_code_scanner">qr_code_scanner</span>
                                    Receive Intake
                                </button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    } catch (e) {
        console.error('Receive queue error:', e);
    }
}

async function receiveParcel(transferId) {
    try {
        const res = await apiFetch(`/api/v1/transfers/${transferId}/receive`, { method: 'PUT' });
        if (!res.ok) {
            const err = await res.json();
            showToast(`Error: ${err.message}`, 'error');
            return;
        }
        showToast(`Transfer #${transferId} received! Marked READY_PICKUP. 48h hold countdown started.`, 'success');
        loadReceiveQueue();
        loadStudentTransfers();
    } catch (e) {
        showToast('Network error during intake', 'error');
    }
}

// Hold Shelf Queue (Parcels in READY_FOR_PICKUP)
async function loadHoldShelfQueue() {
    const container = document.getElementById('hold-shelf-queue-container');
    if (!container) return;
    container.innerHTML = '<div class="text-xs text-outline py-4 text-center">Reading hold shelf inventory...</div>';

    try {
        let url = '/api/v1/transfers/queue?status=READY_FOR_PICKUP';
        if (state.librarianBranchFilter) url += `&branchId=${state.librarianBranchFilter}`;

        const res = await apiFetch(url);
        if (!res.ok) return;
        const queue = await res.json();

        if (queue.length === 0) {
            container.innerHTML = '<div class="p-6 text-center text-xs text-outline bg-surface-container-lowest rounded-2xl border border-outline-variant/30">No parcels currently awaiting pickup on this branch hold shelf.</div>';
            return;
        }

        container.innerHTML = `
            <table class="op-table">
                <thead>
                    <tr>
                        <th>Hold ID</th>
                        <th>Book Volume</th>
                        <th>Barcode</th>
                        <th>Patron</th>
                        <th>Pickup Destination</th>
                        <th>48h Hold Expiry</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    ${queue.map(t => `
                        <tr>
                            <td class="font-mono text-outline">#${t.id}</td>
                            <td><strong class="font-headline text-sm">${escapeHtml(t.bookTitle)}</strong></td>
                            <td><span class="font-mono text-xs font-semibold">${t.barcode}</span></td>
                            <td class="text-xs font-medium text-on-surface">${escapeHtml(t.studentName)} (${t.universityId})</td>
                            <td><span class="px-2.5 py-0.5 rounded-full bg-powder-sage text-primary text-xs font-semibold">${t.toBranchCode} Desk</span></td>
                            <td class="text-xs">
                                <span class="hold-timer" data-expires="${t.expiresAt || ''}">
                                    ⏱️ Hold Active
                                </span>
                            </td>
                            <td>
                                <button class="px-3.5 py-1.5 rounded-full bg-primary-container text-on-primary-container text-xs font-semibold hover:opacity-95 transition-all shadow-sm flex items-center gap-1"
                                        onclick="directHoldCheckout(${t.id}, '${t.barcode}', ${t.studentId || 'null'})">
                                    <span class="material-symbols-outlined text-xs" data-icon="electric_bolt">electric_bolt</span>
                                    1-Click Issue
                                </button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
        tickHoldTimers();

    } catch (e) {
        console.error('Hold shelf load error:', e);
    }
}

async function directHoldCheckout(transferId, barcode, studentId) {
    try {
        const payload = { barcode };
        if (studentId) {
            payload.studentId = parseInt(studentId);
        }

        const res = await apiFetch('/api/v1/loans/checkout', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (res.status === 409) {
            const err = await res.json();
            showToast(`⚠️ Cannot Checkout: ${err.message}`, 'error');
            return;
        }

        if (!res.ok) {
            const err = await res.json();
            showToast(`Error: ${err.message}`, 'error');
            return;
        }

        const loan = await res.json();
        showToast(`⚡ Checkout Successful! Loan #${loan.id} issued for "${loan.bookTitle}". Due: ${loan.dueDate}`, 'success');
        loadHoldShelfQueue();
        loadActiveLoansForReturn();
        loadStudentTransfers();
        loadStudentLoans();
        loadCatalog('');

    } catch (e) {
        showToast('Network error during hold checkout', 'error');
    }
}

async function handleBarcodeCheckout(e) {
    e.preventDefault();
    const barcodeInput = document.getElementById('checkout-barcode-input');
    const barcode = barcodeInput.value.trim();
    const studentSelect = document.getElementById('checkout-student-select');
    const studentIdVal = studentSelect && studentSelect.value ? parseInt(studentSelect.value) : null;

    try {
        const payload = { barcode };
        if (studentIdVal) {
            payload.studentId = studentIdVal;
        }

        const res = await apiFetch('/api/v1/loans/checkout', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (res.status === 409) {
            const err = await res.json();
            showToast(`⚠️ Cannot Checkout: ${err.message}`, 'error');
            return;
        }

        if (!res.ok) {
            const err = await res.json();
            showToast(`Error: ${err.message}`, 'error');
            return;
        }

        const loan = await res.json();
        showToast(`⚡ Checkout Successful! Loan #${loan.id} issued for "${loan.bookTitle}". Due: ${loan.dueDate}`, 'success');
        barcodeInput.value = '';
        loadHoldShelfQueue();
        loadActiveLoansForReturn();
        loadStudentLoans();
        loadCatalog('');

    } catch (e) {
        showToast('Network error during checkout', 'error');
    }
}

async function loadStudentsForCheckout() {
    const select = document.getElementById('checkout-student-select');
    if (!select) return;

    try {
        const res = await apiFetch('/api/v1/loans/students');
        if (!res.ok) return;
        const students = await res.json();
        if (students && students.length > 0) {
            const currentVal = select.value;
            select.innerHTML = '<option value="">-- Auto-Resolve Hold Patron or Select Student --</option>' +
                students.map(s => `<option value="${s.id}">${escapeHtml(s.fullName)} (${s.universityId})</option>`).join('');

            if (currentVal) {
                select.value = currentVal;
            } else {
                const activeStudent = getActiveStudentProfile();
                if (activeStudent && activeStudent.userId) {
                    select.value = String(activeStudent.userId);
                }
            }
        }
    } catch (e) {
        console.error('Checkout students load error:', e);
    }
}

async function loadActiveLoansForReturn() {
    const container = document.getElementById('active-loans-container');
    if (!container) return;
    container.innerHTML = '<div class="text-xs text-outline py-4 text-center">Reading active circulation loans...</div>';

    try {
        const res = await apiFetch('/api/v1/loans/active');
        if (!res.ok) return;
        const loans = await res.json();

        if (loans.length === 0) {
            container.innerHTML = '<div class="p-6 text-center text-xs text-outline bg-surface-container-lowest rounded-2xl border border-outline-variant/30">No active unreturned loans across campus.</div>';
            return;
        }

        container.innerHTML = `
            <table class="op-table">
                <thead>
                    <tr>
                        <th>Loan #</th>
                        <th>Book Volume</th>
                        <th>Barcode</th>
                        <th>Student</th>
                        <th>Due Date</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    ${loans.map(l => `
                        <tr>
                            <td class="font-mono text-outline">#${l.id}</td>
                            <td><strong class="font-headline text-sm">${escapeHtml(l.bookTitle)}</strong></td>
                            <td><span class="font-mono text-xs">${l.barcode}</span></td>
                            <td class="text-xs">${escapeHtml(l.studentName)}</td>
                            <td class="text-xs">${l.dueDate} ${l.isOverdue ? '<span class="text-error font-bold ml-1">(OVERDUE)</span>' : ''}</td>
                            <td>
                                <button class="px-3.5 py-1.5 rounded-full border border-outline-variant/60 text-xs font-semibold text-on-surface hover:bg-surface-container transition-colors flex items-center gap-1" onclick="processReturn(${l.id})">
                                    <span class="material-symbols-outlined text-xs" data-icon="restart_alt">restart_alt</span>
                                    Return Book
                                </button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    } catch (e) {
        console.error('Active loans load error:', e);
    }
}

async function processReturn(loanId) {
    try {
        const res = await apiFetch(`/api/v1/loans/${loanId}/return`, { method: 'POST' });
        if (!res.ok) {
            const err = await res.json();
            showToast(`Error: ${err.message}`, 'error');
            return;
        }

        const loan = await res.json();
        let fineMsg = loan.fineAmount > 0 ? ` (Overdue fine assessed: ₹${loan.fineAmount.toFixed(2)})` : ' (No fines)';
        showToast(`Book returned! Copy restored to AVAILABLE${fineMsg}`, 'success');
        loadActiveLoansForReturn();
        loadStudentLoans();
        loadCatalog('');

    } catch (e) {
        showToast('Network error during return processing', 'error');
    }
}

// ================= PAGE 4: ADMIN AUDIT & FLEET CONTROL =================
async function loadAdminData() {
    await updateAdminTelemetry();
    await loadAuditLogs();
}

async function updateAdminTelemetry() {
    try {
        const res = await apiFetch('/api/v1/books?size=50');
        if (!res.ok) return;
        const pageData = await res.json();
        const books = pageData.content || [];

        let centralAvail = 0, centralTotal = 0;
        let csAvail = 0, csTotal = 0;
        let mechAvail = 0, mechTotal = 0;

        for (const b of books) {
            const inv = state.cachedInventory[b.id];
            if (inv && inv.branchBreakdown) {
                inv.branchBreakdown.forEach(bb => {
                    if (bb.branchCode === 'CENTRAL') {
                        centralAvail += bb.available;
                        centralTotal += bb.totalCopies;
                    } else if (bb.branchCode === 'CS-DEPT') {
                        csAvail += bb.available;
                        csTotal += bb.totalCopies;
                    } else if (bb.branchCode === 'MECH-DEPT') {
                        mechAvail += bb.available;
                        mechTotal += bb.totalCopies;
                    }
                });
            }
        }

        const cEl = document.getElementById('admin-central-metrics');
        const csEl = document.getElementById('admin-cs-metrics');
        const mEl = document.getElementById('admin-mech-metrics');

        if (cEl) cEl.textContent = `${centralAvail} Avail / ${centralTotal} Total`;
        if (csEl) csEl.textContent = `${csAvail} Avail / ${csTotal} Total`;
        if (mEl) mEl.textContent = `${mechAvail} Avail / ${mechTotal} Total`;

    } catch (e) {
        console.error('Admin telemetry calculation error:', e);
    }
}

async function loadAuditLogs() {
    const container = document.getElementById('audit-table-container');
    if (!container) return;
    container.innerHTML = '<div class="text-xs text-outline py-6 text-center">Reading immutable audit trail...</div>';

    try {
        const res = await apiFetch('/api/v1/admin/audit-logs');
        if (!res.ok) {
            container.innerHTML = '<div class="text-xs text-error p-4 text-center">Admin privilege required to inspect immutable audit trail.</div>';
            return;
        }

        state.allAuditLogs = await res.json();
        filterAuditLogs();

    } catch (e) {
        console.error('Audit logs load error:', e);
    }
}

function filterAuditLogs() {
    const searchInput = document.getElementById('audit-search-input');
    const actionSelect = document.getElementById('audit-action-filter');
    const term = searchInput ? searchInput.value.toLowerCase().trim() : '';
    const action = actionSelect ? actionSelect.value : 'ALL';

    let filtered = state.allAuditLogs || [];

    if (action !== 'ALL') {
        filtered = filtered.filter(l => l.action && l.action.toUpperCase().includes(action));
    }

    if (term) {
        filtered = filtered.filter(l => 
            (l.entityName && l.entityName.toLowerCase().includes(term)) ||
            (l.operatorUsername && l.operatorUsername.toLowerCase().includes(term)) ||
            (l.details && l.details.toLowerCase().includes(term))
        );
    }

    renderAuditTable(filtered);
}

function renderAuditTable(logs) {
    const container = document.getElementById('audit-table-container');
    if (!container) return;

    if (!logs || logs.length === 0) {
        container.innerHTML = '<div class="text-xs text-outline p-6 text-center">No audit entries matching filter criteria.</div>';
        return;
    }

    container.innerHTML = `
        <table class="op-table">
            <thead>
                <tr>
                    <th>Timestamp</th>
                    <th>Entity</th>
                    <th>Action</th>
                    <th>Lifecycle Transition</th>
                    <th>Operator</th>
                    <th>Details</th>
                </tr>
            </thead>
            <tbody>
                ${logs.map(l => `
                    <tr>
                        <td class="font-mono text-[11px] text-outline whitespace-nowrap">${l.recordedAt ? l.recordedAt.replace('T', ' ').substring(0, 19) : '-'}</td>
                        <td><strong class="text-xs">${l.entityName} #${l.entityId}</strong></td>
                        <td>
                            <span class="px-2.5 py-0.5 rounded-full bg-powder-sage text-primary text-[11px] font-semibold">
                                ${l.action}
                            </span>
                        </td>
                        <td class="text-xs">
                            ${l.fromState ? `<span class="opacity-70">${l.fromState}</span> ➔ ` : ''}
                            <strong>${l.toState || '-'}</strong>
                        </td>
                        <td class="text-xs font-medium text-on-surface">${escapeHtml(l.operatorUsername || 'SYSTEM')}</td>
                        <td class="text-xs text-outline max-w-xs truncate">${escapeHtml(l.details || '')}</td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

// Live 48-Hour Hold Countdown Timer
function tickHoldTimers() {
    const timerElements = document.querySelectorAll('.hold-timer[data-expires]');
    const now = new Date().getTime();
    timerElements.forEach(el => {
        const expiresStr = el.dataset.expires;
        if (!expiresStr) return;
        const expiry = new Date(expiresStr).getTime();
        const diffMs = expiry - now;
        if (diffMs <= 0) {
            el.innerHTML = '<span class="text-error font-bold">⚠️ Hold Expired</span>';
        } else {
            const hours = Math.floor(diffMs / (1000 * 60 * 60));
            const mins = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
            const secs = Math.floor((diffMs % (1000 * 60)) / 1000);
            el.innerHTML = `⏱️ Expires: <strong class="font-mono text-tertiary">${String(hours).padStart(2, '0')}h ${String(mins).padStart(2, '0')}m ${String(secs).padStart(2, '0')}s</strong>`;
        }
    });
}

// ================= NOTIFICATION TOASTS =================
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let iconName = 'info';
    if (type === 'success') iconName = 'check_circle';
    else if (type === 'error') iconName = 'warning';

    toast.innerHTML = `
        <span class="material-symbols-outlined text-base ${type === 'success' ? 'text-success' : (type === 'error' ? 'text-error' : 'text-secondary')}">${iconName}</span>
        <span class="text-xs font-medium text-on-surface">${escapeHtml(message)}</span>
    `;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(8px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
