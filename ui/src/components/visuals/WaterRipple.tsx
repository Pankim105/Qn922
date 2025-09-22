import { useEffect, useRef } from 'react'

/**
 * WaterRipple 2D 画布水波纹效果
 *
 * 简化版：仅保留点击圆环扩散；
 * - 双缓冲高度图 + 简易平滑
 * - 主题色响应（支持 HSL/HEX/RGB/CSS 变量）
 * - `localEvents`：仅在父容器内响应点击
 */

/**
 * WaterRipple 背景效果（Canvas 2D）
 * - 高度图双缓冲 + 简易扩散/平滑
 * - 移动：依据轨迹段法线在两侧生成"水迹"曲线
 * - 点击：生成向外扩散的圆环（峰/谷）
 */

// ---------- 类型与常量 ----------
type Sample = { x: number; y: number; t: number; weight?: number }

const CTX_2D = '2d' as const
const EVT_MOUSEMOVE = 'mousemove'
const EVT_MOUSEDOWN = 'mousedown'
const EVT_MOUSELEAVE = 'mouseleave'
const EVT_RESIZE = 'resize'

// 绘制参数（只保留点击水波纹相关）
const CLICK_POWER = 2.5
const CLICK_SPEED_BPX_PER_S = 35
const CLICK_THICKNESS = 4

// ---------- 纯工具函数 ----------

/** 将窗口像素坐标映射到 buffer 像素索引（整数） */
function toBufferCoord(x: number, y: number, bw: number, bh: number, canvasRect?: DOMRect) {
  let normalizedX, normalizedY
  
  if (canvasRect) {
    normalizedX = (x - canvasRect.left) / canvasRect.width
    normalizedY = (y - canvasRect.top) / canvasRect.height
  } else {
    normalizedX = x / window.innerWidth
    normalizedY = y / window.innerHeight
  }
  
  const ix = Math.max(0, Math.min(bw - 1, Math.floor(normalizedX * bw)))
  const iy = Math.max(0, Math.min(bh - 1, Math.floor(normalizedY * bh)))
  return { ix, iy }
}

/** 在源缓冲上添加圆形峰/谷（带线性衰减） */
function addRadial(
  src: Float32Array,
  cx: number,
  cy: number,
  radius: number,
  power: number,
  bw: number,
  bh: number,
) {
  for (let dy = -radius; dy <= radius; dy++) {
    for (let dx = -radius; dx <= radius; dx++) {
      const nx = cx + dx, ny = cy + dy
      if (nx >= 0 && nx < bw && ny >= 0 && ny < bh) {
        const dist = Math.hypot(dx, dy)
        if (dist <= radius) {
          const falloff = 1 - dist / radius
          src[ny * bw + nx] += power * falloff
        }
      }
    }
  }
}

/** 稀疏绘制圆环：按角度均匀取样 + 小半径 stamp */
function addRing(
  src: Float32Array,
  cx: number,
  cy: number,
  radius: number,
  thickness: number,
  power: number,
  bw: number,
  bh: number,
) {
  if (radius <= 0) return
  const steps = Math.max(12, Math.ceil(2 * Math.PI * radius * 0.6))
  for (let s = 0; s < steps; s++) {
    const a = (s / steps) * Math.PI * 2
    const x = Math.round(cx + Math.cos(a) * radius)
    const y = Math.round(cy + Math.sin(a) * radius)
    addRadial(src, x, y, thickness, power, bw, bh)
  }
}

// 移除addLine函数，因为不再使用鼠标轨迹

/** 解析颜色字符串为 RGB（支持 var(--x)/#rgb/hsl/rgb） */
function colorToRgb(color: string): { r: number; g: number; b: number } {
  if (color.startsWith('var(')) {
    const varName = color.slice(4, -1)
    const val = getComputedStyle(document.documentElement).getPropertyValue(varName).trim()
    if (!val) return { r: 59, g: 130, b: 246 }
    
    const [hStr, sStr, lStr] = val.split(' ')
    const h = parseFloat(hStr)
    const s = parseFloat(sStr.replace('%','')) / 100
    const l = parseFloat(lStr.replace('%','')) / 100
    const a = s * Math.min(l, 1 - l)
    const f = (n: number) => {
      const k = (n + h / 30) % 12
      const c = l - a * Math.max(-1, Math.min(k - 3, Math.min(9 - k, 1)))
      return Math.round(255 * c)
    }
    return {
      r: f(0),
      g: f(8),
      b: f(4)
    }
  }
  
  if (color.startsWith('#')) {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(color)
    return result ? {
      r: parseInt(result[1], 16),
      g: parseInt(result[2], 16),
      b: parseInt(result[3], 16)
    } : { r: 59, g: 130, b: 246 }
  }
  
  return { r: 59, g: 130, b: 246 }
}

/** 将高度值映射为主题色相关的水波纹颜色 */
function getColorForValue(v: number, themeColor: string = '#3b82f6') {
  const themeRgb = colorToRgb(themeColor)
  const intensity = Math.abs(v)
  const baseAlpha = 60 + intensity * 90
  
  if (v > 0) {
    return {
      r: Math.min(255, themeRgb.r + 70 + intensity * 45),
      g: Math.min(255, themeRgb.g + 50 + intensity * 55),
      b: Math.min(255, themeRgb.b + 30 + intensity * 65),
      a: Math.min(255, baseAlpha * 0.95),
    }
  } else {
    return {
      r: Math.max(0, Math.min(255, themeRgb.r * 0.7 + intensity * 45)),
      g: Math.max(0, Math.min(255, themeRgb.g * 0.75 + intensity * 55)),
      b: Math.max(0, Math.min(255, themeRgb.b * 0.85 + intensity * 65)),
      a: Math.min(255, baseAlpha * 0.9),
    }
  }
}

// ---------- 组件 ----------
interface WaterRippleProps {
  fixed?: boolean
  className?: string
  style?: React.CSSProperties
  localEvents?: boolean
  themeColor?: string
}

export default function WaterRipple({ 
  fixed = false, 
  className, 
  style, 
  localEvents = false, 
  themeColor = '#3b82f6' 
}: WaterRippleProps = {}) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const rafRef = useRef<number | null>(null)
  const wRef = useRef<number>(0)
  const hRef = useRef<number>(0)
  const buffer0 = useRef<Float32Array | null>(null)
  const buffer1 = useRef<Float32Array | null>(null)
  const toggle = useRef<boolean>(false)

  const clicksRef = useRef<Array<Sample & { power: number }>>([])
  // 移除samplesRef和lastMoveTimeRef，因为不再使用轨迹追踪

  useEffect(() => {
    const canvas = canvasRef.current!
    const ctx = canvas.getContext(CTX_2D, { alpha: true }) as CanvasRenderingContext2D
    
    ctx.imageSmoothingEnabled = true
    ctx.imageSmoothingQuality = 'high'

    const resize = () => {
      let width, height, styleWidth, styleHeight
      
      if (fixed) {
        width = Math.floor(window.innerWidth / 2)
        height = Math.floor(window.innerHeight / 2) 
        styleWidth = window.innerWidth
        styleHeight = window.innerHeight
      } else {
        const parent = canvas.parentElement
        if (parent) {
          const rect = parent.getBoundingClientRect()
          width = Math.floor(rect.width / 2) || 200
          height = Math.floor(rect.height / 2) || 100
          styleWidth = rect.width
          styleHeight = rect.height
        } else {
          width = 200
          height = 100
          styleWidth = 400
          styleHeight = 200
        }
      }
      
      wRef.current = width
      hRef.current = height
      canvas.width = width
      canvas.height = height
      canvas.style.width = `${styleWidth}px`
      canvas.style.height = `${styleHeight}px`
      buffer0.current = new Float32Array(width * height)
      buffer1.current = new Float32Array(width * height)
    }
    
    resize()
    window.addEventListener(EVT_RESIZE, resize)

    // 移除鼠标移动轨迹动画，只保留点击水波纹
    const onMove = () => {
      // 不再记录鼠标移动轨迹
      return
    }
    
    const onDown = (e: Event) => {
      const mouseEvent = e as MouseEvent
      let x, y
      
      if (localEvents && cardContainer !== window) {
        const rect = (cardContainer as HTMLElement).getBoundingClientRect()
        x = mouseEvent.clientX - rect.left
        y = mouseEvent.clientY - rect.top
        
        if (x < 0 || y < 0 || x > rect.width || y > rect.height) {
          return
        }
        
        x = mouseEvent.clientX
        y = mouseEvent.clientY
      } else {
        x = mouseEvent.clientX
        y = mouseEvent.clientY
      }
      
      clicksRef.current.push({ x, y, t: performance.now(), power: CLICK_POWER })
    }
    
    const onLeave = () => { 
      // 移除轨迹清理，因为不再使用轨迹追踪
    }
    
    const getCardContainer = () => {
      if (!localEvents) return window
      
      let element = canvas.parentElement
      while (element && element !== document.body) {
        if (element.classList.contains('relative') || element.style.position === 'relative') {
          break
        }
        element = element.parentElement
      }
      
      return element || canvas.parentElement || canvas
    }
    
    const cardContainer = getCardContainer()
    const eventTarget = cardContainer
    
    eventTarget.addEventListener(EVT_MOUSEMOVE, onMove)
    eventTarget.addEventListener(EVT_MOUSEDOWN, onDown)
    if (localEvents && cardContainer !== window) {
      (cardContainer as HTMLElement).addEventListener(EVT_MOUSELEAVE, onLeave)
    } else {
      window.addEventListener(EVT_MOUSELEAVE, onLeave)
    }

    const render = () => {
      const bw = wRef.current, bh = hRef.current
      const src = toggle.current ? buffer1.current! : buffer0.current!
      const dst = toggle.current ? buffer0.current! : buffer1.current!

      src.fill(0)
      dst.fill(0)

      const now = performance.now()

      // 点击圆环注入
      if (clicksRef.current.length) {
        const remain: typeof clicksRef.current = []
        const canvasRect = localEvents && cardContainer !== window ? (cardContainer as HTMLElement).getBoundingClientRect() : undefined
        
        for (const c of clicksRef.current) {
          const age = (now - c.t) / 1000
          const { ix, iy } = toBufferCoord(c.x, c.y, bw, bh, canvasRect)
          const radius = CLICK_SPEED_BPX_PER_S * age
          const amp = c.power * Math.max(0, 1 - age)
          if (amp > 0.1) {
            addRing(src, ix, iy, radius, CLICK_THICKNESS, +amp, bw, bh)
            const innerRadius = Math.max(0, radius - CLICK_THICKNESS * 2)
            const valleyAmp = amp * 0.85
            addRing(src, ix, iy, innerRadius, CLICK_THICKNESS, -valleyAmp, bw, bh)
            remain.push(c)
          }
        }
        clicksRef.current = remain
      }

      // 移除空闲清理，因为不再有轨迹数据

      // 移除轨迹处理，只保留点击水波纹

      // 平滑处理
      for (let y = 1; y < bh - 1; y++) {
        for (let x = 1; x < bw - 1; x++) {
          const idx = y * bw + x
          const sum = src[idx] + src[idx - 1] + src[idx + 1] + src[idx - bw] + src[idx + bw]
          dst[idx] = sum / 5
        }
      }

      // 着色并输出
      const out = ctx.createImageData(bw, bh)
      for (let i = 0; i < src.length; i++) {
        const { r, g, b, a } = getColorForValue(src[i], themeColor)
        const k = i * 4
        out.data[k + 0] = r
        out.data[k + 1] = g
        out.data[k + 2] = b
        out.data[k + 3] = a
      }
      ctx.putImageData(out, 0, 0)

      toggle.current = !toggle.current
      rafRef.current = requestAnimationFrame(render)
    }
    
    rafRef.current = requestAnimationFrame(render)

    return () => {
      if (rafRef.current) cancelAnimationFrame(rafRef.current)
      window.removeEventListener(EVT_RESIZE, resize)
      
      eventTarget.removeEventListener(EVT_MOUSEMOVE, onMove)
      eventTarget.removeEventListener(EVT_MOUSEDOWN, onDown)
      
      if (localEvents && cardContainer !== window) {
        (cardContainer as HTMLElement).removeEventListener(EVT_MOUSELEAVE, onLeave)
      } else {
        window.removeEventListener(EVT_MOUSELEAVE, onLeave)
      }
    }
  }, [localEvents, themeColor])

  const canvasClassName = fixed 
    ? "fixed inset-0 z-0 pointer-events-none select-none rounded-[inherit]"
    : localEvents 
      ? "absolute inset-0 w-full h-full select-none rounded-[inherit]"
      : "absolute inset-0 w-full h-full pointer-events-none select-none rounded-[inherit]"

  const canvasStyle = fixed 
    ? style
    : { 
        mixBlendMode: 'soft-light' as const,
        opacity: 0.7,
        ...style
      }

  return (
    <canvas 
      ref={canvasRef} 
      className={`${canvasClassName} ${className || ''}`}
      style={canvasStyle}
      aria-hidden 
    />
  )
}

