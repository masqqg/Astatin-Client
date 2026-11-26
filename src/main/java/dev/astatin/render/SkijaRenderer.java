package dev.astatin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceOrigin;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Skija renders into an offscreen FBO at the real framebuffer resolution. The
 * result is composited over Minecraft's frame right before the buffer swap,
 * with premultiplied alpha, and every GL state touched here is restored so the
 * 1.21 render pipeline keeps its own cached state intact.
 */
public final class SkijaRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("astatin/render");

    private DirectContext context;
    private Surface surface;
    private BackendRenderTarget target;
    private Render2D ui;
    private int fbo = -1;
    private int colorTexture = -1;
    private int depthStencil = -1;
    private int width;
    private int height;
    private int program = -1;
    private int vao = -1;
    private int vbo = -1;
    private boolean disabled;
    private boolean active;
    private boolean canvasSaved;

    private final IntBuffer intBuffer = BufferUtils.createIntBuffer(16);
    private final FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(4);
    private final ByteBuffer boolBuffer = BufferUtils.createByteBuffer(4);

    private int savedFbo;
    private int savedProgram;
    private int savedVao;
    private int savedArrayBuffer;
    private int savedActiveTexture;
    private int savedTexture0;
    private int savedViewportX;
    private int savedViewportY;
    private int savedViewportW;
    private int savedViewportH;
    private int savedScissorX;
    private int savedScissorY;
    private int savedScissorW;
    private int savedScissorH;
    private int savedBlendSrcRgb;
    private int savedBlendDstRgb;
    private int savedBlendSrcAlpha;
    private int savedBlendDstAlpha;
    private int savedBlendEquationRgb;
    private int savedBlendEquationAlpha;
    private final float[] savedClearColor = new float[4];
    private boolean savedBlend;
    private boolean savedDepth;
    private boolean savedCull;
    private boolean savedScissor;
    private boolean savedSrgb;
    private boolean savedDepthMask;
    private boolean[] savedColorMask = new boolean[4];

    public boolean begin(int framebufferWidth, int framebufferHeight, float guiScale, int guiWidth, int guiHeight) {
        if (disabled || !RenderSystem.isOnRenderThread()) {
            return false;
        }
        active = true;
        canvasSaved = false;
        try {
            saveState();
            ensureSurface(framebufferWidth, framebufferHeight);
            GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fbo);
            GL11C.glViewport(0, 0, framebufferWidth, framebufferHeight);
            GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
            GL11C.glDisable(GL11C.GL_DEPTH_TEST);
            GL11C.glDisable(GL11C.GL_CULL_FACE);
            GL11C.glDisable(GL11C.GL_BLEND);
            GL30C.glDisable(GL30C.GL_FRAMEBUFFER_SRGB);
            GL11C.glClearColor(0f, 0f, 0f, 0f);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT | GL11C.GL_STENCIL_BUFFER_BIT);
            surface.getCanvas().save();
            canvasSaved = true;
            surface.getCanvas().scale(guiScale, guiScale);
            ui.beginFrame(guiWidth, guiHeight);
            return true;
        } catch (Throwable t) {
            disable(t);
            restoreState();
            return false;
        }
    }

    public Render2D ui() {
        return ui;
    }

    public void end() {
        if (!active || disabled) {
            return;
        }
        try {
            ui.endFrame();
            if (canvasSaved) {
                surface.getCanvas().restore();
            }
            context.flushAndSubmit(surface);
            composite();
        } catch (Throwable t) {
            disable(t);
        } finally {
            active = false;
            restoreState();
        }
    }

    private void ensureSurface(int w, int h) {
        if (surface != null && width == w && height == h) {
            return;
        }
        destroySurface();
        width = w;
        height = h;
        if (context == null) {
            context = DirectContext.makeGL();
        }

        fbo = GL30C.glGenFramebuffers();
        colorTexture = GL11C.glGenTextures();
        depthStencil = GL30C.glGenRenderbuffers();

        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, colorTexture);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE);
        GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA8, w, h, 0,
                GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, (ByteBuffer) null);

        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fbo);
        GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
                GL11C.GL_TEXTURE_2D, colorTexture, 0);
        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, depthStencil);
        GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, GL30C.GL_DEPTH24_STENCIL8, w, h);
        GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_STENCIL_ATTACHMENT,
                GL30C.GL_RENDERBUFFER, depthStencil);
        int status = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);
        if (status != GL30C.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Incomplete Skija framebuffer: 0x" + Integer.toHexString(status));
        }

        target = BackendRenderTarget.makeGL(w, h, 0, 8, fbo, colorTexture);
        surface = Surface.wrapBackendRenderTarget(context, target, SurfaceOrigin.BOTTOM_LEFT,
                ColorType.RGBA_8888, ColorSpace.getSRGB());
        ui = new Render2D(surface.getCanvas());
        ensureShader();
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
    }

    private void ensureShader() {
        if (program != -1) {
            return;
        }
        int vertex = compile(GL20C.GL_VERTEX_SHADER, """
                #version 150
                in vec2 aPos;
                out vec2 vUv;
                void main() {
                    vUv = aPos * 0.5 + 0.5;
                    gl_Position = vec4(aPos, 0.0, 1.0);
                }
                """);
        int fragment = compile(GL20C.GL_FRAGMENT_SHADER, """
                #version 150
                uniform sampler2D uTex;
                in vec2 vUv;
                out vec4 fragColor;
                void main() {
                    fragColor = texture(uTex, vUv);
                }
                """);
        program = GL20C.glCreateProgram();
        GL20C.glAttachShader(program, vertex);
        GL20C.glAttachShader(program, fragment);
        GL20C.glBindAttribLocation(program, 0, "aPos");
        GL30C.glBindFragDataLocation(program, 0, "fragColor");
        GL20C.glLinkProgram(program);
        if (GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS) == GL11C.GL_FALSE) {
            throw new IllegalStateException("Skija composite shader failed: " + GL20C.glGetProgramInfoLog(program));
        }
        GL20C.glDeleteShader(vertex);
        GL20C.glDeleteShader(fragment);

        vao = GL30C.glGenVertexArrays();
        vbo = GL15C.glGenBuffers();
        GL30C.glBindVertexArray(vao);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
        float[] vertices = {-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f};
        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, vertices, GL15C.GL_STATIC_DRAW);
        GL20C.glEnableVertexAttribArray(0);
        GL20C.glVertexAttribPointer(0, 2, GL11C.GL_FLOAT, false, 0, 0L);
        GL30C.glBindVertexArray(0);
        GL20C.glUseProgram(program);
        int location = GL20C.glGetUniformLocation(program, "uTex");
        if (location >= 0) {
            GL20C.glUniform1i(location, 0);
        }
        GL20C.glUseProgram(0);
    }

    private int compile(int type, String source) {
        int shader = GL20C.glCreateShader(type);
        GL20C.glShaderSource(shader, source);
        GL20C.glCompileShader(shader);
        if (GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS) == GL11C.GL_FALSE) {
            throw new IllegalStateException("Skija composite shader failed: " + GL20C.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    private void composite() {
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
        GL11C.glViewport(0, 0, width, height);
        GL11C.glDisable(GL11C.GL_DEPTH_TEST);
        GL11C.glDisable(GL11C.GL_CULL_FACE);
        GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
        GL30C.glDisable(GL30C.GL_FRAMEBUFFER_SRGB);
        GL11C.glColorMask(true, true, true, true);
        GL11C.glDepthMask(false);

        GL20C.glUseProgram(program);
        GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, colorTexture);
        GL30C.glBindVertexArray(vao);
        GL11C.glEnable(GL11C.GL_BLEND);
        GL14C.glBlendFuncSeparate(GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA,
                GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA);
        GL11C.glDrawArrays(GL11C.GL_TRIANGLE_STRIP, 0, 4);

        GL11C.glDisable(GL11C.GL_BLEND);
        GL30C.glBindVertexArray(0);
        GL20C.glUseProgram(0);
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, 0);
        GL11C.glDepthMask(true);
    }

    private void saveState() {
        savedActiveTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
        savedTexture0 = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        GL13C.glActiveTexture(savedActiveTexture);

        savedFbo = GL11C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING);
        savedProgram = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        savedVao = GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
        savedArrayBuffer = GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING);
        savedBlend = GL11C.glIsEnabled(GL11C.GL_BLEND);
        savedDepth = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
        savedCull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
        savedScissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
        savedSrgb = GL30C.glIsEnabled(GL30C.GL_FRAMEBUFFER_SRGB);

        intBuffer.clear();
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, intBuffer);
        savedViewportX = intBuffer.get(0);
        savedViewportY = intBuffer.get(1);
        savedViewportW = intBuffer.get(2);
        savedViewportH = intBuffer.get(3);
        intBuffer.clear();
        GL11C.glGetIntegerv(GL11C.GL_SCISSOR_BOX, intBuffer);
        savedScissorX = intBuffer.get(0);
        savedScissorY = intBuffer.get(1);
        savedScissorW = intBuffer.get(2);
        savedScissorH = intBuffer.get(3);

        savedBlendSrcRgb = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_RGB);
        savedBlendDstRgb = GL11C.glGetInteger(GL14C.GL_BLEND_DST_RGB);
        savedBlendSrcAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_ALPHA);
        savedBlendDstAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_DST_ALPHA);
        savedBlendEquationRgb = GL11C.glGetInteger(GL20C.GL_BLEND_EQUATION_RGB);
        savedBlendEquationAlpha = GL11C.glGetInteger(GL20C.GL_BLEND_EQUATION_ALPHA);

        floatBuffer.clear();
        GL11C.glGetFloatv(GL11C.GL_COLOR_CLEAR_VALUE, floatBuffer);
        savedClearColor[0] = floatBuffer.get(0);
        savedClearColor[1] = floatBuffer.get(1);
        savedClearColor[2] = floatBuffer.get(2);
        savedClearColor[3] = floatBuffer.get(3);

        boolBuffer.clear();
        GL11C.glGetBooleanv(GL11C.GL_DEPTH_WRITEMASK, boolBuffer);
        savedDepthMask = boolBuffer.get(0) != 0;
        boolBuffer.clear();
        GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, boolBuffer);
        for (int i = 0; i < 4; i++) {
            savedColorMask[i] = boolBuffer.get(i) != 0;
        }
    }

    private void restoreState() {
        if (savedActiveTexture == 0) {
            return;
        }
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, savedFbo);
        GL11C.glViewport(savedViewportX, savedViewportY, savedViewportW, savedViewportH);
        GL11C.glScissor(savedScissorX, savedScissorY, savedScissorW, savedScissorH);
        GL20C.glUseProgram(savedProgram);
        GL30C.glBindVertexArray(savedVao);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, savedArrayBuffer);
        GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, savedTexture0);
        GL13C.glActiveTexture(savedActiveTexture);
        GL11C.glDepthMask(savedDepthMask);
        GL11C.glColorMask(savedColorMask[0], savedColorMask[1], savedColorMask[2], savedColorMask[3]);
        GL14C.glBlendFuncSeparate(savedBlendSrcRgb, savedBlendDstRgb, savedBlendSrcAlpha, savedBlendDstAlpha);
        GL20C.glBlendEquationSeparate(savedBlendEquationRgb, savedBlendEquationAlpha);
        GL11C.glClearColor(savedClearColor[0], savedClearColor[1], savedClearColor[2], savedClearColor[3]);
        setEnabled(GL11C.GL_BLEND, savedBlend);
        setEnabled(GL11C.GL_DEPTH_TEST, savedDepth);
        setEnabled(GL11C.GL_CULL_FACE, savedCull);
        setEnabled(GL11C.GL_SCISSOR_TEST, savedScissor);
        setEnabled(GL30C.GL_FRAMEBUFFER_SRGB, savedSrgb);
        savedActiveTexture = 0;
    }

    private void setEnabled(int capability, boolean enabled) {
        if (enabled) {
            GL11C.glEnable(capability);
        } else {
            GL11C.glDisable(capability);
        }
    }

    private void destroySurface() {
        if (surface != null) {
            surface.close();
            surface = null;
        }
        if (target != null) {
            target.close();
            target = null;
        }
        if (fbo != -1) {
            GL30C.glDeleteFramebuffers(fbo);
            fbo = -1;
        }
        if (colorTexture != -1) {
            GL11C.glDeleteTextures(colorTexture);
            colorTexture = -1;
        }
        if (depthStencil != -1) {
            GL30C.glDeleteRenderbuffers(depthStencil);
            depthStencil = -1;
        }
    }

    private void disable(Throwable throwable) {
        if (!disabled) {
            disabled = true;
            LOGGER.error("Skija overlay disabled after a rendering error", throwable);
        }
    }

    public boolean isDisabled() {
        return disabled;
    }
}
