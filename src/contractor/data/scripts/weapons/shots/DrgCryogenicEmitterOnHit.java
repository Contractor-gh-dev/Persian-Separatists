package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import contractor.data.scripts.statuseffects.StatusEffect.StatusType;
import contractor.data.scripts.statuseffects.StatusEffectUtils;
import org.lwjgl.util.vector.Vector2f;

public class DrgCryogenicEmitterOnHit implements OnHitEffectPlugin {
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if ((target instanceof ShipAPI ship)) {

			float cold = 0.5f;

			if (shieldHit)
				cold = 0.25f;

			StatusEffectUtils.addOrMaintainEffect(ship, StatusType.CRYOGENIC, cold, 2f);
		}
	}
}
