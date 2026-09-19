package contractor.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;

public class SepNukeEffect implements BeamEffectPlugin {
	private boolean fired = false;

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		ShipAPI source = beam.getSource();
		if (source == null)
			return;

		if (beam.getBrightness() >= 1f && !fired) {
			fired = true;
			WeaponAPI wep = Global.getCombatEngine().createFakeWeapon(source, "sep_dirty_nuke_payload");
			engine.spawnProjectile(source, wep, "sep_dirty_nuke_payload", beam.getFrom(), source.getFacing(), null);

			CombatEntityAPI temp = engine.spawnProjectile(null, null, "sep_dirty_nuke_hull", source.getLocation(), source.getFacing(), source.getVelocity());
			temp.setCollisionClass(CollisionClass.NONE);
			temp.setOwner(100);
		}
	}
}
