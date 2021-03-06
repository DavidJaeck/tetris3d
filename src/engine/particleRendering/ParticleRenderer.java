package engine.particleRendering;

import java.util.List;
import java.util.Map;

import engine.entities.Camera;
import engine.models.RawModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;

import org.lwjgl.util.vector.Vector3f;
import engine.renderEngine.Loader;
import engine.util.Maths;

// all renderer classes are similar:
// the bind and unbind necessary variables and most importantly DRAW to the DISPLAY
public class ParticleRenderer {
    private static final float[] VERTICES = {-0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, -0.5f};
    private final RawModel quad;
    private final ParticleShader shader;

    protected ParticleRenderer(Loader loader, Matrix4f projectionMatrix) {
        quad = loader.loadToVAO(VERTICES, 2);
        shader = new ParticleShader();
        shader.start();
        shader.loadProjectionMatrix(projectionMatrix);
        shader.stop();
    }

    // binds and unbinds Variables enables and disables rendering specific settings and DRAWs the particle
    protected void render(Map<ParticleTexture, List<Particle>> particles, Camera camera) {
        Matrix4f viewMatrix = Maths.createViewMatrix(camera);
        prepare();
        for (ParticleTexture texture : particles.keySet()) { // for all particle engine.textures:
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getTextureID());
            for (Particle particle : particles.get(texture)) { // for all engine.particles with that texture:
                updateModelViewMatrix(particle.getPosition(), particle.getRotation(),
                        particle.getScale(), viewMatrix);
                GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, quad.getVertexCount()); // DRAWS the particles
            }
        }
        finishRendering();
    }

    // computes the ModelViewMatrix here to ensure engine.particles face the camera
    private void updateModelViewMatrix(Vector3f position, float rotation, float scale, Matrix4f viewMatrix) {
        Matrix4f modelMatrix = new Matrix4f();
        Matrix4f.translate(position, modelMatrix, modelMatrix);
        modelMatrix.m00 = viewMatrix.m00;
        modelMatrix.m01 = viewMatrix.m10; // sets the transposed part of the viewMatrix, that is responsible for rotation
        modelMatrix.m02 = viewMatrix.m20;
        modelMatrix.m10 = viewMatrix.m01;
        modelMatrix.m11 = viewMatrix.m11;
        modelMatrix.m12 = viewMatrix.m21;
        modelMatrix.m20 = viewMatrix.m02;
        modelMatrix.m21 = viewMatrix.m12;
        modelMatrix.m22 = viewMatrix.m22;
        Matrix4f.rotate((float) Math.toRadians(rotation), new Vector3f(0, 0, 1), modelMatrix, modelMatrix);
        Matrix4f.scale(new Vector3f(scale, scale, scale), modelMatrix, modelMatrix);
        Matrix4f modelViewMatrix = Matrix4f.mul(viewMatrix, modelMatrix, null); // multiplying M and M^t leaves no rotation
        shader.loadModelViewMatrix(modelViewMatrix);

    }

    private void prepare() {
        shader.start();
        GL30.glBindVertexArray(quad.getVaoID());
        GL20.glEnableVertexAttribArray(0);
        GL11.glEnable(GL11.GL_BLEND); // enable Alpha-blend
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
    }

    private void finishRendering() {
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND); // disable Alpha-blend
        GL20.glDisableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
        shader.stop();
    }

    protected void cleanUp() {
        shader.cleanUp();
    }
}
