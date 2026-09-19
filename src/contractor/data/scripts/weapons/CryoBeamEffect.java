package contractor.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import contractor.data.scripts.statuseffects.StatusEffect;
import contractor.data.scripts.statuseffects.StatusEffectUtils;
import contractor.data.scripts.util.ContractorUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class CryoBeamEffect implements BeamEffectPlugin {
	private static final Color COLD_GLOW = new Color(175, 175, 255, 255);
	private static final Color WEP_GLOW = new Color(125, 125, 255, 200);
	private final IntervalUtil particleInter = new IntervalUtil(0.05f, 0.1f);
	private boolean wasZero = true;
	private boolean readyVent = false;

	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();
		ShipAPI source = beam.getSource();
		if (source == null)
			return;

		float dpsDur = beam.getDamage().getDpsDuration() + 0.05f;
		if (!wasZero) dpsDur = 0;
		wasZero = beam.getDamage().getDpsDuration() <= 0;

		Vector2f randomArea = new Vector2f(MathUtils.getRandomPointInCircle(source.getLocation(), 160f));
		float size = (float) Math.random() * 48f + 16f;
		Global.getCombatEngine().addNebulaParticle(randomArea, source.getVelocity(), size, 0.75f, 0.75f, 0.1f, 1.5f, COLD_GLOW, true);

		particleInter.advance(amount);
		if (particleInter.intervalElapsed()) {
			float angRand1 = (float) (Math.random() - 0.5f) * 8f;
			Vector2f aimVec1 = ContractorUtils.fastUnitVector(beam.getWeapon().getCurrAngle() + angRand1);
			float angRand2 = (float) (Math.random() - 0.5f) * 8f;
			Vector2f aimVec2 = ContractorUtils.fastUnitVector(beam.getWeapon().getCurrAngle() + angRand2);
			VectorUtils.resize(aimVec1, (float) Math.random() * 64f + 512f, aimVec1);
			VectorUtils.resize(aimVec2, (float) Math.random() * 64f + 512f, aimVec2);
			Global.getCombatEngine().addSmokeParticle(beam.getWeapon().getFirePoint(0), aimVec1, size * 0.5f, 0.3f, 1.25f, WEP_GLOW);
			Global.getCombatEngine().addHitParticle(beam.getWeapon().getFirePoint(0), aimVec2, size * 0.6f + 16f, 1f, 0.9f, COLD_GLOW);
		}

		if (readyVent && beam.getBrightness() < 1f) {
			for (WeaponAPI wep : source.getAllWeapons()) {
				if (wep.getId().equals("drg_cryovent_deco")) {
					float angRand = (float) (Math.random() - 0.5f) * 4f;
					Vector2f aimVec = ContractorUtils.fastUnitVector(wep.getCurrAngle() + angRand);
					VectorUtils.resize(aimVec, (float) Math.random() * 64f + 128f, aimVec);
					Global.getCombatEngine().addHitParticle(wep.getFirePoint(0), aimVec, size * 0.4f, 0.75f, 0.4f, COLD_GLOW);
				}
			}
			readyVent = beam.getBrightness() > 0.05f;
		}

		if (beam.getBrightness() >= 1f) {
			readyVent = true;
			if (target instanceof ShipAPI shipTarget) {
				boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getRayEndPrevFrame());
				if (hitShield)
					StatusEffectUtils.addOrMaintainEffect(shipTarget, StatusEffect.StatusType.CRYOGENIC, dpsDur, 0.5f);
				else
					StatusEffectUtils.addOrMaintainEffect(shipTarget, StatusEffect.StatusType.CRYOGENIC, dpsDur * 1.5f, 0.5f);
			}
		}
	}
}
