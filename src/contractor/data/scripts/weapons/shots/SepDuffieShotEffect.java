package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import contractor.data.scripts.plugins.SepFCRGListener;
import contractor.data.scripts.weapons.SepLinkedBarrelSpread;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class SepDuffieShotEffect implements ProximityExplosionEffect, OnFireEffectPlugin, OnHitEffectPlugin, DamageDealtModifier {
	private final Color col1 = new Color(220, 20, 50);
	private final Color col2 = new Color(255, 200, 240);
	private final EmpArcEntityAPI.EmpArcParams param = new EmpArcEntityAPI.EmpArcParams(); {
		param.segmentLengthMult = 12f;
		param.zigZagReductionFactor = 0.3f;
		param.fadeOutDist = 250f;
		param.minFadeOutMult = 2f;
		param.flickerRateMult = 1.25f;
		param.flamesOutMissiles = true;
		param.glowSizeMult = 0.5f;
		param.glowAlphaMult = 0.8f;
	}
	private final float arcRange = 250f, arcThickness = 24f;

	@Override
	public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
		CombatEngineAPI engine = Global.getCombatEngine();
		Vector2f location = new Vector2f(explosion.getLocation());
		for (int i = 0; i < 5; i++) {
			float facing = originalProjectile.getFacing();
			facing += (float) ((Math.random() - 0.5) * 45f);

			engine.spawnProjectile(originalProjectile.getSource(), null, "sep_duffie_spinal_sub", location, facing, new Vector2f(0f, 0f));
		}
		engine.spawnExplosion(location, new Vector2f(0f, 0f), col1, 16f, 0.5f);
		List<ShipAPI> nearbyShip = CombatUtils.getShipsWithinRange(location, arcRange);
		List<MissileAPI> nearbyMissile = CombatUtils.getMissilesWithinRange(location, arcRange);
		EmpArcEntityAPI arc;

		if (!nearbyShip.isEmpty() || !nearbyMissile.isEmpty()) {
			WeightedRandomPicker<CombatEntityAPI> picker = new WeightedRandomPicker<>();
			for (ShipAPI ship : nearbyShip) {
				if (ship.getOwner() != originalProjectile.getOwner())
					picker.add(ship, 2f);
			}
			for (MissileAPI missile : nearbyMissile) {
				if (missile.getOwner() != originalProjectile.getOwner())
					picker.add(missile, 1f);
			}
			CombatEntityAPI choice = picker.pick();

			if (choice == null) {
				Vector2f point = MathUtils.getRandomPointInCircle(location, arcRange);
				arc = engine.spawnEmpArcVisual(location, null, point, null, arcThickness, col1, col2, param);
			} else {
				float radius = choice.getCollisionRadius() + 1f;
				arc = engine.spawnEmpArc(originalProjectile.getSource(), location, null, choice, DamageType.ENERGY, 0f, 150f, arcRange + radius, "pseudoparticle_jet_hit_heavy",
						arcThickness, col1, col2, param);
			}
		} else {
			Vector2f point = MathUtils.getRandomPointInCircle(location, arcRange);
			arc = engine.spawnEmpArcVisual(location, null, point, null, arcThickness, col1, col2, param);
		}
		//arc.setFadedOutAtStart(true);
		arc.setSingleFlickerMode(true);
		arc.setCoreWidthOverride(20f);
		arc.setWarping(0.15f);
	}

	@Override
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		SepLinkedBarrelSpread.LinkOnFireBarrelSpread(weapon);
		ShipAPI ship = weapon.getShip();
		if (!ship.hasListenerOfClass(SepDuffieShotEffect.class))
			ship.addListener(this);
	}

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		engine.spawnEmpArc(projectile.getSource(), point, null, target, DamageType.ENERGY, 0f, 150f, arcRange, "pseudoparticle_jet_hit_heavy",
				arcThickness, col1, col2, param);
		if (!shieldHit && target instanceof ShipAPI targ) {
			if (targ.hasListenerOfClass(SepFCRGListener.class))
				targ.getListeners(SepFCRGListener.class).get(0).reportHit(point);
			else
				targ.addListener(new SepFCRGListener(point, targ));
		}
	}

	@Override
	public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
		if (!shieldHit && param instanceof DamagingProjectileAPI) {
			damage.getModifier().modifyMult("sep_duffie_spinal", 0.67f);
			return "sep_duffie_spinal";
		}
		return null;
	}
}
