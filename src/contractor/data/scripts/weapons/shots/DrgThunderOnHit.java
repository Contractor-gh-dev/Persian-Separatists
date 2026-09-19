package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import contractor.data.scripts.statuseffects.StatusEffect;
import contractor.data.scripts.statuseffects.StatusEffectUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class DrgThunderOnHit implements OnHitEffectPlugin {
	private static final Color col1 = new Color(0, 90, 255);
	private static final Color col2 = new Color(0, 120, 255);
	private static final EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams(); static {
		params.zigZagReductionFactor = 0.1f;
		params.segmentLengthMult = 2f;
		params.fadeOutDist = 200f;
		params.maxZigZagMult = 2f;
		params.glowSizeMult = 2f;
	}

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (shieldHit)
			return;
		if (target instanceof ShipAPI ship) {
			Vector2f start = null;
			if (projectile.getWeapon() != null)
				start = projectile.getWeapon().getFirePoint(0);
			if (start == null)
				return;
			EmpArcEntityAPI arc = engine.spawnEmpArc(projectile.getSource(), start, projectile.getSource(), ship, DamageType.ENERGY, 100f, 100f, 900f + ship.getCollisionRadius(), "mote_attractor_impact_damage",
					48f, col1, col2, params);
			arc.setCoreWidthOverride(32f);
			arc.setFadedOutAtStart(true);
			arc.setSingleFlickerMode();
			StatusEffectUtils.addOrMaintainEffect(ship, StatusEffect.StatusType.ELECTRIC, 4f, 5f);
		}
	}
}
