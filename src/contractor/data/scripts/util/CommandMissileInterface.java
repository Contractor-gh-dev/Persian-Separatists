package contractor.data.scripts.util;

import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.MissileAPI;

public interface CommandMissileInterface extends EveryFrameWeaponEffectPlugin {

	void appendMissile(MissileAPI missile);

	int getActive();
}
