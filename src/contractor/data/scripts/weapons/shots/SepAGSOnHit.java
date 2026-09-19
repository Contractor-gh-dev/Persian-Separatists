package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SepAGSOnHit implements OnHitEffectPlugin {
	private static final Color arcCore = new Color(50, 200, 255, 255);
	private static final Color arcFringe = new Color(150, 200, 255, 100);
	private static final EmpArcEntityAPI.EmpArcParams param = new EmpArcEntityAPI.EmpArcParams(); static {
		param.segmentLengthMult = 3f;
		param.zigZagReductionFactor = 0.1f;
		param.glowColorOverride = arcCore;
		param.flamesOutMissiles = false;
	}

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		for (int i = 0; i < 4; i++) {
			engine.spawnEmpArc(projectile.getSource(), point, target, target, DamageType.ENERGY, 0f, 50f, 128f, null, (float) (Math.random() * 6 + 4f), arcFringe, arcCore, param);
		}
		Global.getSoundPlayer().playSound("mote_attractor_impact_damage", 0.9f, 0.75f, point, target.getVelocity());

		if (!shieldHit)
			return;

		float damage;
		if (target instanceof ShipAPI && target.getShield() != null)
			damage = 150f / (target.getShield().getFluxPerPointOfDamage() * ((ShipAPI) target).getMutableStats().getShieldDamageTakenMult().modified);
		else
			damage = 150f;

		engine.applyDamage(target, point, damage, DamageType.ENERGY, 0f, false, false, projectile, false);

		Global.getSoundPlayer().playSound("mote_attractor_impact_damage", 0.9f, 0.8f, point, target.getVelocity());
		for (int i = 0; i < 4; i++) {
			Vector2f end = MathUtils.getRandomPointInCircle(point, 96f);
			engine.spawnEmpArcVisual(point, target, end, null, (float) (Math.random() * 8 + 8f), arcFringe, arcCore, param);
		}
	}
}
