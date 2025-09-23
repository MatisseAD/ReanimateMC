// Wiki Navigation Script
document.addEventListener('DOMContentLoaded', function() {
    const navLinks = document.querySelectorAll('.nav-link');
    const sections = document.querySelectorAll('.section');
    
    // Function to show a specific section
    function showSection(targetId) {
        // Hide all sections
        sections.forEach(section => {
            section.classList.remove('active');
        });
        
        // Remove active class from all nav links
        navLinks.forEach(link => {
            link.classList.remove('active');
        });
        
        // Show target section
        const targetSection = document.getElementById(targetId);
        if (targetSection) {
            targetSection.classList.add('active');
        }
        
        // Add active class to corresponding nav link
        const activeLink = document.querySelector(`a[href="#${targetId}"]`);
        if (activeLink) {
            activeLink.classList.add('active');
        }
        
        // Scroll to top of content
        document.querySelector('.content').scrollTop = 0;
        
        // Update URL hash without triggering page reload
        history.pushState(null, null, `#${targetId}`);
    }
    
    // Add click event listeners to navigation links
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const targetId = this.getAttribute('href').substring(1);
            showSection(targetId);
        });
    });
    
    // Handle browser back/forward navigation
    window.addEventListener('popstate', function() {
        const hash = window.location.hash.substring(1);
        if (hash) {
            showSection(hash);
        } else {
            showSection('overview');
        }
    });
    
    // Check for initial hash on page load
    const initialHash = window.location.hash.substring(1);
    if (initialHash && document.getElementById(initialHash)) {
        showSection(initialHash);
    } else {
        showSection('overview');
    }
    
    // Smooth scrolling for internal links within sections
    document.addEventListener('click', function(e) {
        if (e.target.tagName === 'A' && e.target.getAttribute('href') && e.target.getAttribute('href').startsWith('#')) {
            const targetId = e.target.getAttribute('href').substring(1);
            const targetElement = document.getElementById(targetId);
            if (targetElement && !e.target.classList.contains('nav-link')) {
                e.preventDefault();
                targetElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        }
    });
    
    // Mobile menu toggle (for future mobile improvements)
    function createMobileToggle() {
        const mobileToggle = document.createElement('button');
        mobileToggle.innerHTML = '☰';
        mobileToggle.className = 'mobile-toggle';
        mobileToggle.style.cssText = `
            display: none;
            position: fixed;
            top: 1rem;
            left: 1rem;
            z-index: 1001;
            background: #667eea;
            color: white;
            border: none;
            padding: 0.5rem;
            border-radius: 4px;
            font-size: 1.2rem;
            cursor: pointer;
        `;
        
        // Show mobile toggle on small screens
        const mediaQuery = window.matchMedia('(max-width: 768px)');
        function handleScreenChange(e) {
            if (e.matches) {
                mobileToggle.style.display = 'block';
                document.body.insertBefore(mobileToggle, document.body.firstChild);
            } else {
                mobileToggle.style.display = 'none';
                if (mobileToggle.parentNode) {
                    mobileToggle.parentNode.removeChild(mobileToggle);
                }
            }
        }
        
        mediaQuery.addListener(handleScreenChange);
        handleScreenChange(mediaQuery);
        
        // Toggle sidebar on mobile
        mobileToggle.addEventListener('click', function() {
            const sidebar = document.querySelector('.sidebar');
            sidebar.classList.toggle('active');
        });
        
        // Close sidebar when clicking on content on mobile
        document.querySelector('.content').addEventListener('click', function() {
            if (window.innerWidth <= 768) {
                const sidebar = document.querySelector('.sidebar');
                sidebar.classList.remove('active');
            }
        });
    }
    
    createMobileToggle();
    
    // Add search functionality
    function createSearchFeature() {
        const searchContainer = document.createElement('div');
        searchContainer.innerHTML = `
            <div class="search-container" style="padding: 1rem 1.5rem; border-bottom: 1px solid rgba(255, 255, 255, 0.1);">
                <input type="text" id="wiki-search" placeholder="Search wiki..." style="
                    width: 100%;
                    padding: 0.5rem;
                    border: 1px solid rgba(255, 255, 255, 0.3);
                    border-radius: 4px;
                    background: rgba(255, 255, 255, 0.1);
                    color: white;
                    font-size: 0.9rem;
                ">
            </div>
        `;
        
        const sidebar = document.querySelector('.sidebar');
        const logo = document.querySelector('.logo');
        sidebar.insertBefore(searchContainer, logo.nextSibling);
        
        const searchInput = document.getElementById('wiki-search');
        
        searchInput.addEventListener('input', function() {
            const searchTerm = this.value.toLowerCase();
            const navLinks = document.querySelectorAll('.nav-link');
            
            navLinks.forEach(link => {
                const text = link.textContent.toLowerCase();
                const listItem = link.parentElement;
                
                if (text.includes(searchTerm)) {
                    listItem.style.display = 'block';
                } else {
                    listItem.style.display = searchTerm === '' ? 'block' : 'none';
                }
            });
        });
        
        // Style the search input placeholder
        searchInput.addEventListener('focus', function() {
            this.style.background = 'rgba(255, 255, 255, 0.2)';
        });
        
        searchInput.addEventListener('blur', function() {
            this.style.background = 'rgba(255, 255, 255, 0.1)';
        });
    }
    
    createSearchFeature();
    
    // Add copy-to-clipboard functionality for code elements
    function addCopyFunctionality() {
        const codeElements = document.querySelectorAll('code');
        
        codeElements.forEach(code => {
            if (code.textContent.length > 10) { // Only add to longer code snippets
                code.style.position = 'relative';
                code.style.cursor = 'pointer';
                code.title = 'Click to copy';
                
                code.addEventListener('click', function() {
                    navigator.clipboard.writeText(this.textContent).then(() => {
                        // Show temporary feedback
                        const feedback = document.createElement('span');
                        feedback.textContent = 'Copied!';
                        feedback.style.cssText = `
                            position: absolute;
                            top: -25px;
                            left: 50%;
                            transform: translateX(-50%);
                            background: #28a745;
                            color: white;
                            padding: 4px 8px;
                            border-radius: 4px;
                            font-size: 12px;
                            z-index: 1000;
                        `;
                        
                        this.appendChild(feedback);
                        setTimeout(() => {
                            if (feedback.parentNode) {
                                feedback.parentNode.removeChild(feedback);
                            }
                        }, 2000);
                    });
                });
            }
        });
    }
    
    addCopyFunctionality();
    
    // Add keyboard navigation
    document.addEventListener('keydown', function(e) {
        if (e.altKey) {
            const currentActive = document.querySelector('.nav-link.active');
            const allNavLinks = Array.from(document.querySelectorAll('.nav-link'));
            const currentIndex = allNavLinks.indexOf(currentActive);
            
            if (e.key === 'ArrowDown' && currentIndex < allNavLinks.length - 1) {
                e.preventDefault();
                allNavLinks[currentIndex + 1].click();
            } else if (e.key === 'ArrowUp' && currentIndex > 0) {
                e.preventDefault();
                allNavLinks[currentIndex - 1].click();
            }
        }
    });
    
    // Add a "Back to Top" button for long sections
    function createBackToTopButton() {
        const backToTop = document.createElement('button');
        backToTop.innerHTML = '↑';
        backToTop.className = 'back-to-top';
        backToTop.style.cssText = `
            position: fixed;
            bottom: 2rem;
            right: 2rem;
            background: #4CAF50;
            color: white;
            border: none;
            border-radius: 50%;
            width: 50px;
            height: 50px;
            font-size: 1.5rem;
            cursor: pointer;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
            opacity: 0;
            transition: opacity 0.3s ease;
            z-index: 1000;
        `;
        
        document.body.appendChild(backToTop);
        
        // Show/hide based on scroll position
        const content = document.querySelector('.content');
        content.addEventListener('scroll', function() {
            if (this.scrollTop > 500) {
                backToTop.style.opacity = '1';
            } else {
                backToTop.style.opacity = '0';
            }
        });
        
        backToTop.addEventListener('click', function() {
            content.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        });
    }
    
    createBackToTopButton();
    
    // Add loading animation for external images
    function handleImageLoading() {
        const images = document.querySelectorAll('img');
        
        images.forEach(img => {
            if (!img.complete) {
                img.style.opacity = '0';
                img.style.transition = 'opacity 0.3s ease';
                
                img.addEventListener('load', function() {
                    this.style.opacity = '1';
                });
                
                img.addEventListener('error', function() {
                    this.style.opacity = '0.5';
                    this.style.filter = 'grayscale(100%)';
                });
            }
        });
    }
    
    handleImageLoading();
    
    console.log('ReanimateMC Wiki loaded successfully!');
});