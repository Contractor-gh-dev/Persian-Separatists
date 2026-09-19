package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import contractor.data.scripts.plugins.DrgNeedleListener;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class DrgNeedleOnHit implements OnHitEffectPlugin {
	private final Color col1 = new Color(150, 30, 150, 255);

	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (shieldHit) {
			engine.spawnExplosion(point, new Vector2f(0f, 0f), col1, 48f, 0.75f);
			return;
		}

		if (target instanceof ShipAPI) {
			Vector2f hitInShipCoords = new Vector2f(Vector2f.sub(point, target.getLocation(), null));
			float needleFacing = projectile.getFacing();
			float targetFacing = target.getFacing();

			if (!((ShipAPI) target).hasListenerOfClass(DrgNeedleListener.class))
				((ShipAPI) target).addListener(new DrgNeedleListener(needleFacing, targetFacing, hitInShipCoords, (ShipAPI) target, projectile.getSource()));
			else
				((ShipAPI) target).getListeners(DrgNeedleListener.class).get(0).addNeedle(needleFacing, targetFacing, hitInShipCoords, projectile.getSource());
		} else {
			engine.spawnDamagingExplosion(DrgNeedleListener.HULLSPEC, projectile.getSource(), point, true);
		}
	}
}