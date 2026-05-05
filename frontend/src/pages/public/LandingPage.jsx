// src/pages/public/LandingPage.jsx
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as THREE from 'three';
import './LandingPage.css';

const SOUND_SRC = 'https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3';

export default function LandingPage() {
  const navigate  = useNavigate();
  const mountRef  = useRef(null);
  const sceneRef  = useRef({});
  const scrollRef = useRef(0);
  const rafRef    = useRef(null);
  const audioRef  = useRef(null);

  const [progress,  setProgress]  = useState(0);
  const [showEnter, setShowEnter] = useState(false);
  const [muted,     setMuted]     = useState(true);
  const [entered,   setEntered]   = useState(false);

  useEffect(() => {
    if (localStorage.getItem('zn_skip_landing') === '1') {
      navigate('/home', { replace: true });
    }
  }, [navigate]);

  useEffect(() => {
    const audio = new Audio(SOUND_SRC);
    audio.loop = true; audio.volume = 0.25; audio.muted = true;
    audioRef.current = audio;
    return () => { audio.pause(); audio.src = ''; };
  }, []);

  const toggleMute = () => {
    setMuted(m => {
      const next = !m;
      if (audioRef.current) {
        audioRef.current.muted = next;
        if (!next) audioRef.current.play().catch(() => {});
      }
      return next;
    });
  };

  useEffect(() => {
    const el = mountRef.current;
    if (!el) return;

    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setSize(el.clientWidth, el.clientHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.2;
    el.appendChild(renderer.domElement);

    const scene  = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(45, el.clientWidth / el.clientHeight, 0.1, 100);
    camera.position.set(0, 0.3, 5);
    scene.fog = new THREE.FogExp2(0x000408, 0.08);

    scene.add(new THREE.AmbientLight(0x0a0a1a, 1));
    const rimLight = new THREE.DirectionalLight(0x4488ff, 1.5);
    rimLight.position.set(-3, 2, -2);
    scene.add(rimLight);
    const bladeGlow = new THREE.PointLight(0x88ccff, 0, 4);
    scene.add(bladeGlow);
    const redFill = new THREE.PointLight(0xff1144, 0.5, 6);
    redFill.position.set(2, -1, 2);
    scene.add(redFill);

    const sheathMat = new THREE.MeshStandardMaterial({ color: 0x0d0d12, roughness: 0.3, metalness: 0.6 });
    const goldMat   = (r = 0.2) => new THREE.MeshStandardMaterial({ color: 0xc8a84b, roughness: r, metalness: 0.9 });

    const sheathGroup = new THREE.Group();
    sheathGroup.add(new THREE.Mesh(new THREE.CylinderGeometry(0.045, 0.038, 2.6, 12), sheathMat));
    const mouth = new THREE.Mesh(new THREE.CylinderGeometry(0.055, 0.055, 0.06, 12), goldMat());
    mouth.position.y = 1.31;
    sheathGroup.add(mouth);
    const cap = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.03, 0.1, 12), goldMat());
    cap.position.y = -1.35;
    sheathGroup.add(cap);
    [-0.4, 0.2, 0.8].forEach(y => {
      const band = new THREE.Mesh(new THREE.CylinderGeometry(0.048, 0.048, 0.04, 12), goldMat(0.15));
      band.position.y = y;
      sheathGroup.add(band);
    });
    sheathGroup.rotation.z = Math.PI / 2;
    sheathGroup.rotation.x = 0.1;
    scene.add(sheathGroup);

    const bladeGroup = new THREE.Group();
    const bladeShape = new THREE.Shape();
    bladeShape.moveTo(0,0); bladeShape.lineTo(0.018,0.1); bladeShape.lineTo(0.012,2.4);
    bladeShape.lineTo(0,2.5); bladeShape.lineTo(-0.002,2.4); bladeShape.lineTo(-0.002,0.1); bladeShape.lineTo(0,0);
    const blade = new THREE.Mesh(
      new THREE.ExtrudeGeometry(bladeShape, { depth:0.004, bevelEnabled:true, bevelThickness:0.001, bevelSize:0.001, bevelSegments:1 }),
      new THREE.MeshStandardMaterial({ color:0xddeeff, roughness:0.05, metalness:1.0 })
    );
    blade.rotation.z = -Math.PI / 2;
    bladeGroup.add(blade);
    const hamon = new THREE.Mesh(
      new THREE.PlaneGeometry(2.3, 0.003),
      new THREE.MeshBasicMaterial({ color:0xaaddff, transparent:true, opacity:0.6, side:THREE.DoubleSide })
    );
    hamon.position.set(1.15, -0.006, 0.003);
    bladeGroup.add(hamon);
    const tsuba = new THREE.Mesh(new THREE.TorusGeometry(0.07, 0.018, 8, 24), goldMat(0.1));
    tsuba.rotation.y = Math.PI / 2;
    bladeGroup.add(tsuba);
    const tsuka = new THREE.Mesh(new THREE.CylinderGeometry(0.025,0.022,0.55,12), new THREE.MeshStandardMaterial({ color:0x1a0a08, roughness:0.8, metalness:0.1 }));
    tsuka.rotation.z = Math.PI / 2; tsuka.position.x = -0.3;
    bladeGroup.add(tsuka);
    for (let i = 0; i < 8; i++) {
      const wrap = new THREE.Mesh(new THREE.TorusGeometry(0.027,0.003,6,16), new THREE.MeshStandardMaterial({ color:0x2a1510, roughness:1 }));
      wrap.rotation.y = Math.PI / 2; wrap.position.x = -0.08 - i * 0.06;
      bladeGroup.add(wrap);
    }
    const kashira = new THREE.Mesh(new THREE.SphereGeometry(0.03,12,8), goldMat(0.1));
    kashira.position.x = -0.57;
    bladeGroup.add(kashira);
    bladeGroup.rotation.z = Math.PI / 2;
    bladeGroup.rotation.x = 0.1;
    bladeGroup.position.set(-1.35, 0, 0);
    scene.add(bladeGroup);

    const PARTS = 1200;
    const pos = new Float32Array(PARTS*3), col = new Float32Array(PARTS*3);
    for (let i = 0; i < PARTS; i++) {
      pos[i*3]=(Math.random()-0.5)*20; pos[i*3+1]=(Math.random()-0.5)*12; pos[i*3+2]=(Math.random()-0.5)*10-2;
      const t=Math.random(); col[i*3]=0.1+t*0.2; col[i*3+1]=0.3+t*0.4; col[i*3+2]=0.8+t*0.2;
    }
    const partGeo = new THREE.BufferGeometry();
    partGeo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
    partGeo.setAttribute('color',    new THREE.BufferAttribute(col, 3));
    const particles = new THREE.Points(partGeo, new THREE.PointsMaterial({ size:0.04, vertexColors:true, transparent:true, opacity:0.7, sizeAttenuation:true, depthWrite:false }));
    scene.add(particles);

    const petalCount = 60, petalData = [], petalPos = new Float32Array(petalCount*3);
    const petalGeo = new THREE.BufferGeometry();
    for (let i = 0; i < petalCount; i++) {
      petalData.push({ x:(Math.random()-0.5)*14, y:Math.random()*10+3, z:(Math.random()-0.5)*6-1, vy:-(Math.random()*0.008+0.004), vx:(Math.random()-0.5)*0.003, phase:Math.random()*Math.PI*2 });
      petalPos[i*3]=petalData[i].x; petalPos[i*3+1]=petalData[i].y; petalPos[i*3+2]=petalData[i].z;
    }
    petalGeo.setAttribute('position', new THREE.BufferAttribute(petalPos, 3));
    const petals = new THREE.Points(petalGeo, new THREE.PointsMaterial({ color:0xffaacc, size:0.07, transparent:true, opacity:0.55, depthWrite:false, sizeAttenuation:true }));
    scene.add(petals);

    sceneRef.current = { renderer, scene, camera, bladeGroup, sheathGroup, bladeGlow, particles, petals, petalData, petalGeo };

    const onResize = () => {
      const w = el.clientWidth, h = el.clientHeight;
      camera.aspect = w/h; camera.updateProjectionMatrix(); renderer.setSize(w,h);
    };
    window.addEventListener('resize', onResize);

    let frame = 0;
    const animate = () => {
      rafRef.current = requestAnimationFrame(animate);
      frame++;
      const p = scrollRef.current;
      bladeGroup.position.x   = -1.35 + p * 2.8;
      bladeGroup.rotation.x   = 0.1 - p * 0.05;
      sheathGroup.rotation.x  = 0.1 + p * 0.05;
      bladeGlow.intensity      = p * 3.5;
      bladeGlow.position.x     = bladeGroup.position.x;
      camera.position.x = Math.sin(frame*0.003)*0.3;
      camera.position.y = 0.3 + Math.cos(frame*0.002)*0.15 + p*0.4;
      camera.position.z = 5 - p*1.2;
      camera.lookAt(0,0,0);
      particles.rotation.y += 0.0003; particles.rotation.x += 0.0001;
      particles.material.opacity = 0.4 + p*0.4;
      const pp = petalGeo.attributes.position.array;
      for (let i = 0; i < petalCount; i++) {
        const d = petalData[i];
        d.y += d.vy; d.x += d.vx + Math.sin(frame*0.01+d.phase)*0.002;
        if (d.y < -6) { d.y=8; d.x=(Math.random()-0.5)*14; }
        pp[i*3]=d.x; pp[i*3+1]=d.y; pp[i*3+2]=d.z;
      }
      petalGeo.attributes.position.needsUpdate = true;
      petals.material.opacity = 0.3 + p*0.4;
      renderer.render(scene, camera);
    };
    animate();

    return () => {
      cancelAnimationFrame(rafRef.current);
      window.removeEventListener('resize', onResize);
      renderer.dispose();
      if (el.contains(renderer.domElement)) el.removeChild(renderer.domElement);
    };
  }, []);

  useEffect(() => {
    const onWheel = (e) => {
      if (entered) return;
      scrollRef.current = Math.min(1, Math.max(0, scrollRef.current + e.deltaY/1200));
      const p = scrollRef.current;
      setProgress(p);
      setShowEnter(p >= 0.98);
      if (audioRef.current && p > 0.01) audioRef.current.play().catch(() => {});
    };
    let touchStart = 0;
    const onTouchStart = (e) => { touchStart = e.touches[0].clientY; };
    const onTouchMove  = (e) => {
      if (entered) return;
      const delta = (touchStart - e.touches[0].clientY)/500;
      touchStart = e.touches[0].clientY;
      scrollRef.current = Math.min(1, Math.max(0, scrollRef.current + delta));
      const p = scrollRef.current;
      setProgress(p); setShowEnter(p >= 0.98);
    };
    window.addEventListener('wheel',      onWheel,      { passive: true });
    window.addEventListener('touchstart', onTouchStart, { passive: true });
    window.addEventListener('touchmove',  onTouchMove,  { passive: true });
    return () => {
      window.removeEventListener('wheel',      onWheel);
      window.removeEventListener('touchstart', onTouchStart);
      window.removeEventListener('touchmove',  onTouchMove);
    };
  }, [entered]);

  const handleEnter = () => {
    setEntered(true);
    localStorage.setItem('zn_skip_landing', '1');
    if (audioRef.current) audioRef.current.pause();
    navigate('/home');
  };

  const handleSkip = () => {
    localStorage.setItem('zn_skip_landing', '1');
    if (audioRef.current) audioRef.current.pause();
    navigate('/home');
  };

  const statusText =
    progress < 0.05 ? 'SCROLL TO UNSHEATHE' :
    progress < 0.50 ? 'DRAWING THE BLADE...' :
    progress < 0.98 ? 'ALMOST REVEALED...' : 'BLADE DRAWN';

  return (
    <div className="lp-root">
      <div ref={mountRef} className="lp-canvas" />
      <div className="lp-vignette" />
      <div className="lp-scanlines" />

      <div className="lp-top-bar">
        <div className="lp-logo">Z<span className="lp-logo-accent">N</span></div>
        <div className="lp-top-bar-right">
          <button className="lp-icon-btn" onClick={toggleMute} title={muted ? 'Unmute' : 'Mute'}>
            {muted ? '🔇' : '🔊'}
          </button>
          <button className="lp-skip-btn" onClick={handleSkip}>Skip intro</button>
        </div>
      </div>

      <div className="lp-center">
        <div className="lp-title-wrap" style={{ opacity: 1 - progress * 0.4 }}>
          <p className="lp-subtitle">&#8212; WHERE ANIME LIVES &#8212;</p>
          <h1 className="lp-title">
            {'ZENENATION'.split('').map((ch, i) => (
              <span key={i}
                className={`lp-title-char${i === 4 ? ' lp-title-char--gold' : ''}`}
                style={{ animationDelay: `${i * 0.07}s` }}>
                {ch}
              </span>
            ))}
          </h1>
          <p className="lp-scroll-hint" style={{ opacity: progress < 0.05 ? 1 : Math.max(0, 1 - progress * 4) }}>
            scroll to reveal
          </p>
        </div>

        <svg className="lp-progress-arc" viewBox="0 0 120 120">
          <circle cx="60" cy="60" r="54" fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="1.5" />
          <circle cx="60" cy="60" r="54" fill="none" stroke="#c8a84b" strokeWidth="1.5"
            strokeDasharray={`${progress * 339.3} 339.3`} strokeLinecap="round"
            transform="rotate(-90 60 60)" style={{ transition: 'stroke-dasharray 0.1s ease' }} />
          <text x="60" y="65" textAnchor="middle" fill="rgba(255,255,255,0.5)"
            fontSize="12" fontFamily="'Courier New', monospace" letterSpacing="1">
            {Math.round(progress * 100)}%
          </text>
        </svg>
      </div>

      <div className="lp-enter-wrap" style={{ opacity: showEnter ? 1 : 0, pointerEvents: showEnter ? 'all' : 'none' }}>
        <div className="lp-enter-glow" />
        <button className="lp-enter-btn" onClick={handleEnter}>
          <span className="lp-enter-btn-text">ENTER THE WORLD</span>
          <span className="lp-enter-btn-arrow">&#8594;</span>
        </button>
        <p className="lp-enter-sub">The blade is drawn. Step forward.</p>
      </div>

      <div className="lp-bottom-bar">
        <div className="lp-status-dot" />
        <span className="lp-status-text">{statusText}</span>
      </div>
    </div>
  );
}