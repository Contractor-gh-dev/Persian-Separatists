package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SepShredderProx implements ProximityExplosionEffect {
	@Override
	public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
		CombatEngineAPI engine = Global.getCombatEngine();
		int shards;
		float dam = originalProjectile.getBaseDamageAmount();
		shards = ((int) dam / 10);
		if (((int) dam % 10) == 0) {
			shards -= 1;
			if (shards < 1)
				return;
		}

		ShipAPI source = originalProjectile.getSource();
		Vector2f loc = originalProjectile.getLocation();
		for (int i = 0; i < shards; i++) {
			float facing = originalProjectile.getFacing();
			facing += (float) ((Math.random() - 0.5) * 150f);
			engine.spawnProjectile(source, null, "sep_lightcycler_sub", loc, facing, null);
		}
		engine.spawnExplosion(loc, new Vector2f(0f, 0f), Color.GRAY, 16f, 0.5f);
	}
}
