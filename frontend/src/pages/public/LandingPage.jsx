/// src/pages/public/LandingPage.jsx

import { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import * as THREE from "three";
import "./LandingPage.css";

export default function LandingPage() {

  const mountRef = useRef(null);
  const scrollProgress = useRef(0);

  // ✅ ADD THIS
  const navigate = useNavigate();

  useEffect(() => {

    const container = mountRef.current;

    /* ---------------------------------------------------
       SCENE
    --------------------------------------------------- */

    const scene = new THREE.Scene();

    scene.fog = new THREE.FogExp2(0x05070b, 0.12);

    const camera = new THREE.PerspectiveCamera(
      42,
      container.clientWidth / container.clientHeight,
      0.1,
      100
    );

    camera.position.set(0, 0.2, 6);

    const renderer = new THREE.WebGLRenderer({
      antialias: true,
      alpha: true,
    });

    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

    renderer.setSize(
      container.clientWidth,
      container.clientHeight
    );

    renderer.outputColorSpace =
      THREE.SRGBColorSpace;

    container.appendChild(renderer.domElement);

    /* ---------------------------------------------------
       LIGHTS
    --------------------------------------------------- */

    scene.add(new THREE.AmbientLight(0xffffff, 0.4));

    const rim = new THREE.DirectionalLight(
      0x7aa2ff,
      2
    );

    rim.position.set(-3, 2, 4);

    scene.add(rim);

    const gold = new THREE.PointLight(
      0xc8a84b,
      8,
      12
    );

    gold.position.set(0, 0, 2);

    scene.add(gold);

    /* ---------------------------------------------------
       PARTICLES
    --------------------------------------------------- */

    const particlesGeo = new THREE.BufferGeometry();

    const count = 2000;

    const pos = new Float32Array(count * 3);

    for (let i = 0; i < count * 3; i++) {
      pos[i] = (Math.random() - 0.5) * 20;
    }

    particlesGeo.setAttribute(
      "position",
      new THREE.BufferAttribute(pos, 3)
    );

    const particlesMat =
      new THREE.PointsMaterial({
        size: 0.02,
        transparent: true,
        opacity: 0.5,
        color: "#88aaff",
        depthWrite: false,
      });

    const particles = new THREE.Points(
      particlesGeo,
      particlesMat
    );

    scene.add(particles);

    /* ---------------------------------------------------
       KATANA
    --------------------------------------------------- */

    const katana = new THREE.Group();

    scene.add(katana);

    const sheathGroup = new THREE.Group();

    const sheathMat =
      new THREE.MeshPhysicalMaterial({
        color: 0x070707,
        roughness: 0.35,
        metalness: 0.7,
        clearcoat: 1,
      });

    const sheath = new THREE.Mesh(
      new THREE.CylinderGeometry(
        0.09,
        0.085,
        4.2,
        32
      ),
      sheathMat
    );

    sheath.rotation.z = Math.PI / 2;

    sheathGroup.add(sheath);

    const ringMat =
      new THREE.MeshPhysicalMaterial({
        color: 0xc8a84b,
        metalness: 1,
        roughness: 0.15,
      });

    [-1.4, -0.5, 0.5, 1.4].forEach((x) => {

      const ring = new THREE.Mesh(
        new THREE.TorusGeometry(
          0.1,
          0.02,
          12,
          32
        ),
        ringMat
      );

      ring.rotation.y = Math.PI / 2;

      ring.position.x = x;

      sheathGroup.add(ring);

    });

    katana.add(sheathGroup);

    /* ---------------------------------------------------
       BLADE
    --------------------------------------------------- */

    const bladeGroup = new THREE.Group();

    const bladeShape = new THREE.Shape();

    bladeShape.moveTo(0, 0);

    bladeShape.lineTo(0.02, 0.2);

    bladeShape.lineTo(0.028, 3.6);

    bladeShape.lineTo(0, 3.9);

    bladeShape.lineTo(-0.008, 3.6);

    bladeShape.lineTo(-0.01, 0.2);

    bladeShape.lineTo(0, 0);

    const bladeGeo =
      new THREE.ExtrudeGeometry(
        bladeShape,
        {
          depth: 0.012,
          bevelEnabled: true,
          bevelThickness: 0.003,
          bevelSize: 0.002,
          bevelSegments: 2,
        }
      );

    const bladeMat =
      new THREE.MeshPhysicalMaterial({
        color: 0xe7f1ff,
        metalness: 1,
        roughness: 0.12,
        clearcoat: 1,
        reflectivity: 1,
      });

    const blade = new THREE.Mesh(
      bladeGeo,
      bladeMat
    );

    blade.rotation.z = -Math.PI / 2;

    blade.position.x = -1.9;

    bladeGroup.add(blade);

    const hamon = new THREE.Mesh(
      new THREE.PlaneGeometry(3.2, 0.015),
      new THREE.MeshBasicMaterial({
        color: 0x9ed0ff,
        transparent: true,
        opacity: 0.7,
      })
    );

    hamon.rotation.z = -Math.PI / 2;

    hamon.position.set(0, 1.7, 0.02);

    bladeGroup.add(hamon);

    const guard = new THREE.Mesh(
      new THREE.TorusGeometry(
        0.12,
        0.03,
        12,
        40
      ),
      ringMat
    );

    guard.rotation.y = Math.PI / 2;

    bladeGroup.add(guard);

    const handle = new THREE.Mesh(
      new THREE.CylinderGeometry(
        0.045,
        0.045,
        0.8,
        20
      ),
      new THREE.MeshStandardMaterial({
        color: 0x120b09,
        roughness: 1,
      })
    );

    handle.rotation.z = Math.PI / 2;

    handle.position.x = -0.55;

    bladeGroup.add(handle);

    katana.add(bladeGroup);

    katana.rotation.z = 0.05;

    katana.rotation.x = 0.1;

    /* ---------------------------------------------------
       SCROLL
    --------------------------------------------------- */

    const updateScroll = () => {

      const max =
        document.body.scrollHeight -
        window.innerHeight;

      scrollProgress.current =
        window.scrollY / max;

    };

    window.addEventListener(
      "scroll",
      updateScroll
    );

    /* ---------------------------------------------------
       ANIMATION
    --------------------------------------------------- */

    const animate = () => {

      requestAnimationFrame(animate);

      const p = scrollProgress.current;

      bladeGroup.position.x = p * 3.2;

      katana.rotation.z =
        0.05 - p * 0.15;

      katana.position.y =
        p * 0.3;

      camera.position.z =
        6 - p * 1.5;

      camera.position.y =
        p * 0.4;

      particles.rotation.y += 0.0002;

      particles.material.opacity =
        0.25 + p * 0.5;

      gold.intensity =
        4 + p * 12;

      renderer.render(scene, camera);

    };

    animate();

    /* ---------------------------------------------------
       RESIZE
    --------------------------------------------------- */

    const handleResize = () => {

      camera.aspect =
        container.clientWidth /
        container.clientHeight;

      camera.updateProjectionMatrix();

      renderer.setSize(
        container.clientWidth,
        container.clientHeight
      );

    };

    window.addEventListener(
      "resize",
      handleResize
    );

    return () => {

      window.removeEventListener(
        "resize",
        handleResize
      );

      window.removeEventListener(
        "scroll",
        updateScroll
      );

      renderer.dispose();

      if (
        container &&
        container.contains(renderer.domElement)
      ) {
        container.removeChild(
          renderer.domElement
        );
      }

    };

  }, []);

  return (
    <div className="story-root">

      <div
        className="three-container"
        ref={mountRef}
      ></div>

      <section className="hero-section">

        <p className="hero-kicker">
          THE LEGEND BEGINS
        </p>

        <h1>
          Draw the blade.
          <br />
          Enter the world.
        </h1>

        <p className="hero-sub">
          Scroll to slowly unsheathe the katana
          and reveal the story of ZeneNation.
        </p>

      </section>

      <section className="story-section">

        <div className="story-card">

          <span>01</span>

          <h2>
            The silence before battle.
          </h2>

          <p>
            In darkness, every legend waits.
            The blade sleeps within the sheath.
          </p>

        </div>

      </section>

      <section className="story-section">

        <div className="story-card">

          <span>02</span>

          <h2>
            The steel awakens.
          </h2>

          <p>
            With every scroll, the katana
            reveals more of its spirit.
          </p>

        </div>

      </section>

      <section className="story-section">

        <div className="story-card">

          <span>03</span>

          <h2>
            The world opens.
          </h2>

          <p>
            Anime. Stories. Characters.
            Worlds waiting to be explored.
          </p>

        </div>

      </section>

      {/* ✅ FINAL SECTION */}

      <section className="final-section">

        <h2>
          ZeneNation awaits.
        </h2>

        <button
          className="enter-btn"
          onClick={() => navigate("/home")}
        >
          ENTER THE WORLD
        </button>

      </section>

    </div>
  );
}


// src/pages/public/LandingPage.jsx
// import { useEffect, useState } from 'react';
// import { useNavigate } from 'react-router-dom';
// import { useAuth } from '../../context/AuthContext';
// import './LandingPage.css';

// export default function LandingPage() {
//   const navigate = useNavigate();
//   const { user, loading } = useAuth();
//   const [loaded, setLoaded] = useState(false);

//   useEffect(() => {
//     // Wait for auth to finish loading
//     if (loading) return;

//     // If already visited AND logged in → go home
//     // If already visited AND not logged in → show landing again
//     if (localStorage.getItem('zn_skip_landing') === '1' && user) {
//       navigate('/home', { replace: true });
//       return;
//     }

//     // If already visited but not logged in — reset flag and show landing
//     if (localStorage.getItem('zn_skip_landing') === '1' && !user) {
//       localStorage.removeItem('zn_skip_landing');
//     }

//     const t = setTimeout(() => setLoaded(true), 100);
//     return () => clearTimeout(t);
//   }, [navigate, user, loading]);

//   const handleEnter = () => {
//     localStorage.setItem('zn_skip_landing', '1');
//     navigate('/home');
//   };

//   if (loading) return null;

//   return (
//     <div className={`lp-root ${loaded ? 'lp-loaded' : ''}`}>

//       {/* ── Left Panel — Artwork ── */}
//       <div className="lp-left">
//         <div className="lp-left-bg" />
//         <div className="lp-grid-overlay" />

//         <div className="lp-orb lp-orb-1" />
//         <div className="lp-orb lp-orb-2" />
//         <div className="lp-orb lp-orb-3" />

//         <div className="lp-artwork">
//           <div className="lp-ring lp-ring-outer" />
//           <div className="lp-ring lp-ring-mid" />
//           <div className="lp-ring lp-ring-inner" />
//           <div className="lp-katana-wrap">
//             <div className="lp-katana">
//               <div className="lp-blade" />
//               <div className="lp-guard" />
//               <div className="lp-handle" />
//             </div>
//           </div>
//           <div className="lp-glow-burst" />
//         </div>

//         <div className="lp-tag lp-tag-1">Figures</div>
//         <div className="lp-tag lp-tag-2">Weapons</div>
//         <div className="lp-tag lp-tag-3">Apparel</div>
//         <div className="lp-tag lp-tag-4">Collectibles</div>

//         <div className="lp-marquee-wrap">
//           <div className="lp-marquee">
//             {Array(6).fill('ZENENATION · ANIME · COLLECTIBLES · ').map((t, i) => (
//               <span key={i}>{t}</span>
//             ))}
//           </div>
//         </div>
//       </div>

//       {/* ── Right Panel — Content ── */}
//       <div className="lp-right">
//         <div className="lp-right-inner">

//           <div className="lp-logo">
//             <span className="lp-logo-z">Z</span>
//             <span className="lp-logo-rest">ENENATION</span>
//           </div>

//           <p className="lp-eyebrow">&#8212; Est. 2025 &middot; India&apos;s Anime Store &#8212;</p>

//           <h1 className="lp-headline">
//             <span className="lp-headline-line">Where</span>
//             <span className="lp-headline-line lp-headline-accent">Anime</span>
//             <span className="lp-headline-line">Lives.</span>
//           </h1>

//           <p className="lp-desc">
//             Premium figures, katanas, apparel and collectibles —
//             curated for true fans. Every piece tells a story.
//           </p>

//           <div className="lp-stats">
//             <div className="lp-stat">
//               <span className="lp-stat-num">500+</span>
//               <span className="lp-stat-label">Products</span>
//             </div>
//             <div className="lp-stat-divider" />
//             <div className="lp-stat">
//               <span className="lp-stat-num">50+</span>
//               <span className="lp-stat-label">Brands</span>
//             </div>
//             <div className="lp-stat-divider" />
//             <div className="lp-stat">
//               <span className="lp-stat-num">10K+</span>
//               <span className="lp-stat-label">Fans</span>
//             </div>
//           </div>

//           <div className="lp-cta-wrap">
//             <button className="lp-cta-btn" onClick={handleEnter}>
//               <span>Get Started</span>
//               <svg className="lp-cta-arrow" viewBox="0 0 24 24" fill="none">
//                 <path d="M5 12h14M13 6l6 6-6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
//               </svg>
//             </button>
//             <button className="lp-skip-btn" onClick={handleEnter}>
//               Skip intro
//             </button>
//           </div>

//           <div className="lp-categories">
//             {['Naruto', 'One Piece', 'Demon Slayer', 'Attack on Titan', 'Jujutsu Kaisen'].map((cat, i) => (
//               <span key={i} className="lp-cat-chip">{cat}</span>
//             ))}
//           </div>

//         </div>

//         <div className="lp-corner-tl" />
//         <div className="lp-corner-br" />
//       </div>

//     </div>
//   );
// }