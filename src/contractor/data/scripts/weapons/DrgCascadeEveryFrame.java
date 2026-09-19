package contractor.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class DrgCascadeEveryFrame implements EveryFrameWeaponEffectPlugin {
	private static final Vector2f[] TURRET_OFFSETS = {
			new Vector2f(21f, -7.5f),
			new Vector2f(21f, 7.5f)
	};
	private static final Vector2f[] HARDPOINT_OFFSETS = {
			new Vector2f(25f, -7.5f),
			new Vector2f(25f, 7.5f)
	};
	private static final Color glow = new Color(175, 40, 110, 255);
	private boolean hasFired = false;
	//private float glowAmount = 0f;

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (engine.isPaused())
			return;
		if (weapon.getChargeLevel() > 0.9f) {
			hasFired = true;
			//glowAmount = 1f;
		} //else {
		//glowAmount -= (float) (amount * 0.5);
		//}
		//weapon.setGlowAmount(glowAmount, glow);
		if (hasFired && weapon.getChargeLevel() <= 0.01f) {
			weapon.ensureClonedSpec();
			WeaponSpecAPI spec = weapon.getSpec();
			hasFired = false;
			if (weapon.getSlot().isHardpoint()) {
				if (spec.getHardpointFireOffsets().get(0).equals(HARDPOINT_OFFSETS[0]))
					spec.getHardpointFireOffsets().set(0, HARDPOINT_OFFSETS[1]);
				else
					spec.getHardpointFireOffsets().set(0, HARDPOINT_OFFSETS[0]);

			} else {
				if (spec.getTurretFireOffsets().get(0).equals(TURRET_OFFSETS[0]))
					spec.getTurretFireOffsets().set(0, TURRET_OFFSETS[1]);
				else
					spec.getTurretFireOffsets().set(0, TURRET_OFFSETS[0]);
			}
		}
	}
}
