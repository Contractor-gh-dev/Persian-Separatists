package contractor.data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Noise;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Arrays;
import java.util.EnumSet;

public class DrgGabrielBeamRender extends BaseCombatLayeredRenderingPlugin {
	protected FaderUtil fader;
	protected SpriteAPI atmosphereTex;

	protected float[] noise;
	protected float[] noise1;
	protected float[] noise2;

	protected NegativeExplosionVisual.NEParams p;

	protected int segments;
	protected float noiseElapsed = 0f;

	protected WeaponAPI sourceWep;

	public DrgGabrielBeamRender(NegativeExplosionVisual.NEParams p, WeaponAPI sourceWep) {
		this.p = p;
		this.sourceWep = sourceWep;
	}

	public float getRenderRadius() {
		return p.radius + 500f;
	}

	@Override
	public EnumSet<CombatEngineLayers> getActiveLayers() {
		return EnumSet.of(CombatEngineLayers.ABOVE_PARTICLES_LOWER, CombatEngineLayers.ABOVE_PARTICLES);
	}

	public FaderUtil getFader() {
		return fader;
	}

	public void init(CombatEntityAPI entity) {
		super.init(entity);

		fader = new FaderUtil(0f, p.fadeIn, p.fadeOut);
		fader.setBounceDown(true);
		fader.fadeIn();

		atmosphereTex = Global.getSettings().getSprite("combat", "corona_hard");

		float perSegment = 2f;
		segments = (int) ((p.radius * 1.5f * 3.14f) / perSegment);
		if (segments < 8) segments = 8;

		noise1 = Noise.genNoise(segments, p.noiseMag);
		noise2 = Noise.genNoise(segments, p.noiseMag);
		noise = Arrays.copyOf(noise1, noise1.length);
	}

	public boolean isExpired() {
		return fader.isFadedOut();
	}

	public void advance(float amount) {
		if (Global.getCombatEngine().isPaused()) return;

		fader.advance(amount);

		if (p.noiseMag > 0) {
			noiseElapsed += amount;
			if (noiseElapsed > p.noisePeriod) {
				noiseElapsed = 0;
				noise1 = Arrays.copyOf(noise2, noise2.length);
				noise2 = Noise.genNoise(segments, p.noiseMag);
			}
			float f = noiseElapsed / p.noisePeriod;
			for (int i = 0; i < noise.length; i++) {
				float n1 = noise1[i];
				float n2 = noise2[i];
				noise[i] = n1 + (n2 - n1) * f;
			}
		}
		if (sourceWep.getCooldownRemaining() > 0)
			fader.setState(FaderUtil.State.OUT);
	}

	public void render(CombatEngineLayers layer, ViewportAPI viewport) {
		float x = entity.getLocation().x;
		float y = entity.getLocation().y;

		float f = fader.getBrightness();
		float alphaMult = viewport.getAlphaMult();

		float r = p.radius;

		if (fader.isFadingIn()) {
			r *= 0.1f + 0.9f * (f * f);
		} else {
			r *= 0.1f + 0.9f * f;
		}

		if (layer == CombatEngineLayers.ABOVE_PARTICLES_LOWER) {
			float circleAlpha = 0.1f + 0.9f * (f * f);
			if (alphaMult < 0.5f) {
				circleAlpha = alphaMult * 2f;
			}
			renderAtmosphere(x, y, r * 1.1f, p.thickness, circleAlpha, segments, atmosphereTex, noise, p.underglow, true);
			renderAtmosphere(x, y, r - 2f, p.thickness, circleAlpha, segments, atmosphereTex, noise, p.underglow, true);
		}

		if (layer == CombatEngineLayers.ABOVE_PARTICLES) {
			float circleAlpha = (f * f);
			if (alphaMult < 0.5f) {
				circleAlpha = alphaMult * 2f;
			}
			float tCircleBorder = 1f;
			renderCircle(x, y, r, circleAlpha, segments, p.blackColor);
			renderAtmosphere(x, y, r, tCircleBorder, circleAlpha, segments, atmosphereTex, noise, p.blackColor, p.additiveBlend);
		}
	}

	private void renderCircle(float x, float y, float radius, float alphaMult, int segments, Color color) {
		if (fader.isFadingIn() && p.blackColor == Color.black) alphaMult = 1f;

		float startRad = (float) Math.toRadians(0);
		float endRad = (float) Math.toRadians(360);
		float spanRad = Misc.normalizeAngle(endRad - startRad);
		float anglePerSegment = spanRad / segments;

		GL11.glPushMatrix();
		GL11.glTranslatef(x, y, 0);
		GL11.glRotatef(0, 0, 0, 1);
		GL11.glDisable(GL11.GL_TEXTURE_2D);

		GL11.glEnable(GL11.GL_BLEND);
		if (p.additiveBlend) {
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		} else {
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		}

		GL11.glColor4ub((byte) color.getRed(),
				(byte) color.getGreen(),
				(byte) color.getBlue(),
				(byte) ((float) color.getAlpha() * alphaMult));

		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glVertex2f(0, 0);
		for (float i = 0; i < segments + 1; i++) {
			boolean last = i == segments;
			if (last) i = 0;
			float theta = anglePerSegment * i;
			float cos = (float) Math.cos(theta);
			float sin = (float) Math.sin(theta);

			float m1 = 0.75f + 0.65f * noise[(int) i] * p.noiseMult;
			if (p.noiseMag <= 0) {
				m1 = 1f;
			}

			float x1 = cos * radius * m1;
			float y1 = sin * radius * m1;

			GL11.glVertex2f(x1, y1);

			if (last) break;
		}

		GL11.glEnd();
		GL11.glPopMatrix();
	}

	private void renderAtmosphere(float x, float y, float radius, float thickness, float alphaMult, int segments, SpriteAPI tex, float[] noise, Color color, boolean additive) {

		float startRad = (float) Math.toRadians(0);
		float endRad = (float) Math.toRadians(360);
		float spanRad = Misc.normalizeAngle(endRad - startRad);
		float anglePerSegment = spanRad / segments;

		GL11.glPushMatrix();
		GL11.glTranslatef(x, y, 0);
		GL11.glRotatef(0, 0, 0, 1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);

		tex.bindTexture();

		GL11.glEnable(GL11.GL_BLEND);
		if (additive) {
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		} else {
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		}

		GL11.glColor4ub((byte) color.getRed(),
				(byte) color.getGreen(),
				(byte) color.getBlue(),
				(byte) ((float) color.getAlpha() * alphaMult));
		float texX = 0f;
		float incr = 1f / segments;
		GL11.glBegin(GL11.GL_QUAD_STRIP);
		for (float i = 0; i < segments + 1; i++) {
			boolean last = i == segments;
			if (last) i = 0;
			float theta = anglePerSegment * i;
			float cos = (float) Math.cos(theta);
			float sin = (float) Math.sin(theta);

			float m1 = 0.75f + 0.65f * noise[(int) i] * p.noiseMult;
			float m2 = m1;
			if (p.noiseMag <= 0) {
				m1 = 1f;
				m2 = 1f;
			}

			float x1 = cos * radius * m1;
			float y1 = sin * radius * m1;
			float x2 = cos * (radius + thickness * m2);
			float y2 = sin * (radius + thickness * m2);

			GL11.glTexCoord2f(0.5f, 0.05f);
			GL11.glVertex2f(x1, y1);

			GL11.glTexCoord2f(0.5f, 0.95f);
			GL11.glVertex2f(x2, y2);

			texX += incr;
			if (last) break;
		}

		GL11.glEnd();
		GL11.glPopMatrix();
	}
}
