// Hàm xử lý hiệu ứng nhảy số
const animateCounters = () => {
  const counters = document.querySelectorAll(".counter-value");
  const speed = 200; // Tốc độ chạy (số càng nhỏ chạy càng nhanh)

  counters.forEach((counter) => {
    const updateCount = () => {
      const target = +counter.getAttribute("data-target");
      const count = +counter.innerText;

      // Tính toán bước nhảy
      const inc = target / speed;

      // Nếu số hiện tại nhỏ hơn mục tiêu, tiếp tục cộng dồn
      if (count < target) {
        counter.innerText = Math.ceil(count + inc);
        setTimeout(updateCount, 15); // Lặp lại sau 15ms
      } else {
        counter.innerText = target; // Đảm bảo số dừng chính xác ở mục tiêu
      }
    };
    updateCount();
  });
};

// Sử dụng IntersectionObserver để chỉ chạy hiệu ứng khi cuộn tới vùng chứa số
const observer = new IntersectionObserver(
  (entries, observer) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        animateCounters();
        observer.unobserve(entry.target); // Chỉ chạy 1 lần
      }
    });
  },
  { threshold: 0.5 },
); // Kích hoạt khi ít nhất 50% khu vực thống kê hiển thị trên màn hình

// Lắng nghe phần tử chứa các con số
const counterSection = document.getElementById("counter-section");
if (counterSection) {
  observer.observe(counterSection);
}
