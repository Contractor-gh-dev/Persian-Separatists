package contractor.data.scripts.statuseffects;

import com.fs.starfarer.api.combat.ShipAPI;
import org.jetbrains.annotations.NotNull;

public class BaseStatusEffect implements StatusEffect {
	protected StatusType type = StatusType.OTHER;
	protected final ShipAPI target;
	protected String id;
	protected float effectLevel;

	/**
	 * @param target Ship to attach listener to.
	 * @param initial Initial value of effectLevel, generally in seconds.
	 */
	public BaseStatusEffect(@NotNull ShipAPI target, float initial) {
		this.target = target;
		this.id = target.getId() + StatusType.OTHER;
		this.effectLevel = initial;
	}

	public void addEffectLevel(float effectLevel) {
		this.effectLevel += effectLevel;
	}

	public void setEffectLevel(float effectLevel) {
		this.effectLevel = effectLevel;
	}

	public float getEffectLevel() {
		return effectLevel;
	}

	public void remove() {
		target.removeListener(this);
	}

	public void remove(boolean withImmunity) {
		if (withImmunity && !target.hasListenerOfClass(Immune.class))
			target.addListener(new Immune(target, 5f, false));
		remove();
	}

	public StatusType getStatusType() {
		return type;
	}

	public void advance(float amount) {
		if (!target.isAlive()) {
			remove(false);
			return;
		}

		effectLevel -= amount;
		if (effectLevel <= 0f)
			remove(false);
	}
}
