// Language switcher functionality
function changeLanguage(lang) {
    const currentPage = window.location.pathname.split('/').pop() || 'index.html';
    const pageName = currentPage.replace('.html', '');
    
    // Store language preference
    localStorage.setItem('preferredLanguage', lang);
    
    // Redirect to language-specific page
    if (lang !== 'en') {
        window.location.href = `${lang}/${pageName}.html`;
    } else {
        // English is the default, no subfolder
        window.location.href = `${pageName}.html`;
    }
}

// Set language selector to current language on page load
document.addEventListener('DOMContentLoaded', function() {
    const preferredLang = localStorage.getItem('preferredLanguage') || 'en';
    const langSelect = document.getElementById('lang-select');
    
    if (langSelect) {
        // Detect current language from URL
        const currentPath = window.location.pathname;
        const langMatch = currentPath.match(/\/(en|fr|es|de|it|ru|zh|kr|pt)\//);
        const currentLang = langMatch ? langMatch[1] : 'en';
        
        langSelect.value = currentLang;
    }
    
    // Add smooth scrolling to all links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
    
    // Add fade-in animation to sections on scroll
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };
    
    const observer = new IntersectionObserver(function(entries) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('fade-in');
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);
    
    document.querySelectorAll('.section').forEach(section => {
        observer.observe(section);
    });
    
    // Mobile menu toggle
    const navToggle = document.querySelector('.nav-toggle');
    const navLinks = document.querySelector('.nav-links');
    
    if (navToggle && navLinks) {
        navToggle.addEventListener('click', function() {
            navLinks.classList.toggle('active');
            const icon = navToggle.querySelector('i');
            if (icon) {
                icon.classList.toggle('fa-bars');
                icon.classList.toggle('fa-times');
            }
        });

        // Close menu when clicking on a link
        navLinks.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', function() {
                if (window.innerWidth <= 768) {
                    navLinks.classList.remove('active');
                    const icon = navToggle.querySelector('i');
                    if (icon) {
                        icon.classList.add('fa-bars');
                        icon.classList.remove('fa-times');
                    }
                }
            });
        });

        // Close menu when clicking outside
        document.addEventListener('click', function(event) {
            if (window.innerWidth <= 768) {
                const isClickInsideNav = navToggle.contains(event.target) || navLinks.contains(event.target);
                if (!isClickInsideNav && navLinks.classList.contains('active')) {
                    navLinks.classList.remove('active');
                    const icon = navToggle.querySelector('i');
                    if (icon) {
                        icon.classList.add('fa-bars');
                        icon.classList.remove('fa-times');
                    }
                }
            }
        });
    }
    
    // Copy code blocks on click
    document.querySelectorAll('pre code').forEach(block => {
        block.addEventListener('click', function() {
            const text = this.textContent;
            navigator.clipboard.writeText(text).then(function() {
                // Show copied notification
                const notification = document.createElement('div');
                notification.textContent = 'Copied!';
                notification.style.cssText = 'position:fixed;top:20px;right:20px;background:#27ae60;color:white;padding:1rem 2rem;border-radius:8px;z-index:9999;';
                document.body.appendChild(notification);
                setTimeout(() => notification.remove(), 2000);
            });
        });
    });
});

// Search functionality (basic implementation)
function initSearch() {
    const searchInput = document.getElementById('search-input');
    if (!searchInput) return;
    
    searchInput.addEventListener('input', function(e) {
        const query = e.target.value.toLowerCase();
        const searchResults = document.getElementById('search-results');
        
        if (query.length < 2) {
            searchResults.innerHTML = '';
            return;
        }
        
        // Simple search implementation - can be enhanced with a proper search library
        const allContent = document.querySelectorAll('section, .feature-card, .resource-card');
        const results = [];
        
        allContent.forEach(element => {
            const text = element.textContent.toLowerCase();
            if (text.includes(query)) {
                const title = element.querySelector('h2, h3')?.textContent || 'Result';
                results.push({
                    title: title,
                    element: element
                });
            }
        });
        
        displaySearchResults(results, searchResults);
    });
}

function displaySearchResults(results, container) {
    if (results.length === 0) {
        container.innerHTML = '<p>No results found</p>';
        return;
    }
    
    container.innerHTML = results.slice(0, 5).map(result => `
        <div class="search-result" onclick="scrollToElement(this.dataset.id)">
            <h4>${result.title}</h4>
        </div>
    `).join('');
}

function scrollToElement(id) {
    const element = document.getElementById(id);
    if (element) {
        element.scrollIntoView({ behavior: 'smooth' });
    }
}

// Initialize search when DOM is ready
document.addEventListener('DOMContentLoaded', initSearch);

// Back to top button
function addBackToTopButton() {
    const button = document.createElement('button');
    button.innerHTML = '<i class="fas fa-arrow-up"></i>';
    button.className = 'back-to-top';
    button.style.cssText = `
        position: fixed;
        bottom: 20px;
        right: 20px;
        width: 50px;
        height: 50px;
        border-radius: 50%;
        background: linear-gradient(135deg, #e74c3c, #c0392b);
        color: white;
        border: none;
        cursor: pointer;
        display: none;
        align-items: center;
        justify-content: center;
        font-size: 1.2rem;
        box-shadow: 0 4px 10px rgba(0,0,0,0.2);
        z-index: 1000;
        transition: all 0.3s;
    `;
    
    button.addEventListener('click', function() {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });
    
    window.addEventListener('scroll', function() {
        if (window.pageYOffset > 300) {
            button.style.display = 'flex';
        } else {
            button.style.display = 'none';
        }
    });
    
    document.body.appendChild(button);
}

document.addEventListener('DOMContentLoaded', addBackToTopButton);
