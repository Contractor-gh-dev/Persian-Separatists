package contractor.data.scripts.statuseffects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class Cryogenic extends BaseStatusEffect {
	private float radius;
	private static final Color coldGlow = new Color(150, 150, 255, 200);
	private float time = 0f;
	private int decay = 1;

	public Cryogenic(@NotNull ShipAPI target, float initial) {
		super(target, initial);
		this.id = target.getId() + StatusType.CRYOGENIC;
		this.type = StatusType.CRYOGENIC;
		radius = target.getSpriteAPI().getWidth();
		radius += target.getSpriteAPI().getHeight();
		radius *= 0.25f;
	}

	@Override
	public void advance(float amount) {
		if (!target.isAlive()) {
			remove();
			return;
		}
		boolean isPlayer = target == Global.getCombatEngine().getPlayerShip();

		float timeMult;
		if (isPlayer)
			timeMult = 1f;
		else
			timeMult = target.getMutableStats().getTimeMult().modified;

		time += amount * timeMult;
		if (time >= 30f)
			decay = 3;
		else if (time >= 15f)
			decay = 2;

		float debuff = 0f, brightFrac = 0.1f, sizeFrac = 36f;
		String data = "LOW SPEED REDUCTION";

		if (effectLevel >= 10f && time <= 15f) {
			debuff = 0.75f;
			data = "HIGH SPEED REDUCTION";
			brightFrac = 0.3f;
			sizeFrac = 48f;
		} else if (effectLevel >= 5f && time <= 30f) {
			debuff = 0.8f;
			data = "MEDIUM SPEED REDUCTION";
			brightFrac = 0.2f;
			sizeFrac = 42f;
		} else if (effectLevel < 5f || time > 30f) {
			debuff = 0.9f;
			data = "LOW SPEED REDUCTION";
			brightFrac = 0.1f;
			sizeFrac = 36f;
		}

		Vector2f randomArea = new Vector2f(MathUtils.getRandomPointInCircle(target.getLocation(), radius));
		float size = (float) Math.random() * sizeFrac + 32f;
		Global.getCombatEngine().addNebulaParticle(randomArea, target.getVelocity(), size, 0.75f, 0.5f, brightFrac, 2f, coldGlow, true);

		target.getMutableStats().getMaxSpeed().modifyMult(id, debuff);
		target.getMutableStats().getAcceleration().modifyMult(id, debuff);
		target.getMutableStats().getDeceleration().modifyMult(id, debuff);
		target.getMutableStats().getTurnAcceleration().modifyMult(id, debuff);
		target.getMutableStats().getMaxTurnRate().modifyMult(id, debuff);

		if (isPlayer)
			Global.getCombatEngine().maintainStatusForPlayerShip(id, Global.getSettings().getSpriteName("systems", "drg_cryo_ui_sprite"), "Cryogenic Freeze", data, true);

		effectLevel -= amount * decay * timeMult;
		if (effectLevel <= 0f)
			remove();
	}

	@Override
	public void addEffectLevel(float effectLevel) {
		this.effectLevel += effectLevel;
		if (this.effectLevel > 30f)
			this.effectLevel = 30f;
	}

	@Override
	public void remove() {
		target.getMutableStats().getMaxSpeed().unmodify(id);
		target.getMutableStats().getAcceleration().unmodify(id);
		target.getMutableStats().getDeceleration().unmodify(id);
		target.getMutableStats().getTurnAcceleration().unmodify(id);
		target.getMutableStats().getMaxTurnRate().unmodify(id);
		target.removeListener(this);
	}
}
