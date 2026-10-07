document.addEventListener("DOMContentLoaded", () => {
  const observerOptions = {
    threshold: 0.15,
    rootMargin: "0px 0px -50px 0px"
  };

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('show-animate');
        observer.unobserve(entry.target); // Chỉ chạy 1 lần khi cuộn tới
      }
    });
  }, observerOptions);

  const hiddenElements = document.querySelectorAll('.animate-hidden');
  hiddenElements.forEach((el) => observer.observe(el));
});
