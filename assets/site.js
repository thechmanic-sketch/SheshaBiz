(function () {
  // Scroll-reveal
  var targets = document.querySelectorAll(".reveal, .reveal-stagger");
  if ("IntersectionObserver" in window && targets.length) {
    var io = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            entry.target.classList.add("is-visible");
            io.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.15, rootMargin: "0px 0px -40px 0px" }
    );
    targets.forEach(function (t) { io.observe(t); });
  } else {
    targets.forEach(function (t) { t.classList.add("is-visible"); });
  }

  // Hero phone slider
  var slider = document.querySelector("[data-phone-slider]");
  if (slider) {
    var slides = Array.prototype.slice.call(slider.querySelectorAll(".slide"));
    var dotsWrap = document.querySelector("[data-phone-dots]");
    var caption = document.querySelector("[data-phone-caption]");
    var idx = 0;
    var timer = null;
    var reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    var dots = slides.map(function (slide, i) {
      var b = document.createElement("button");
      b.type = "button";
      b.setAttribute("aria-label", "Show screen " + (i + 1));
      if (i === 0) b.classList.add("is-active");
      b.addEventListener("click", function () { show(i); restart(); });
      dotsWrap.appendChild(b);
      return b;
    });

    function show(i) {
      slides[idx].classList.remove("is-active");
      dots[idx].classList.remove("is-active");
      idx = i;
      slides[idx].classList.add("is-active");
      dots[idx].classList.add("is-active");
      if (caption) caption.textContent = slides[idx].getAttribute("data-caption") || "";
    }

    function next() { show((idx + 1) % slides.length); }

    function restart() {
      if (timer) clearInterval(timer);
      if (!reduced) timer = setInterval(next, 4200);
    }

    if (caption) caption.textContent = slides[0].getAttribute("data-caption") || "";
    restart();
  }
})();
