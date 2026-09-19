package contractor.data.scripts.statuseffects;

import com.fs.starfarer.api.combat.ShipAPI;
import org.jetbrains.annotations.NotNull;

public class Immune extends BaseStatusEffect {

	public Immune(@NotNull ShipAPI target, float initial, boolean clearEffects) {
		super(target, initial);
		this.type = StatusType.IMMUNE;
		this.id = target.getId() + StatusType.IMMUNE;
	}
}
