package de.teamlapen.vampirism.entity;

import de.teamlapen.vampirism.ModPotion;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.entity.player.VampirePlayer;
import de.teamlapen.vampirism.util.BALANCE;
import de.teamlapen.vampirism.util.Logger;
import de.teamlapen.vampirism.util.REFERENCE;
import de.teamlapen.vampirism.villages.VillageVampire;
import de.teamlapen.vampirism.villages.VillageVampireData;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIBreakDoor;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Abstract class which already implements some vampire specific things, like sundamage
 * @author maxanier
 *
 */
public abstract class DefaultVampire extends EntityMob {

	protected float sundamage=0.5f;
	
	/**
	 * Already sets a few tasks like attacking player, hunter, villager. First new task should have the priority 4 or higher
	 * @param world
	 */
	public DefaultVampire(World world) {
		super(world);
		
		this.getNavigator().setAvoidsWater(true);
		this.getNavigator().setBreakDoors(true);
		this.setSize(0.6F, 1.8F);

		this.tasks.addTask(0, new EntityAISwimming(this));
		this.tasks.addTask(1, new EntityAIBreakDoor(this));
		this.tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.1, false));
		this.tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityVampireHunter.class, 1.0, true));
		this.tasks.addTask(3, new EntityAIAttackOnCollide(this, EntityVillager.class, 0.9, true));
	}
	
	@Override
	public boolean isAIEnabled() {
		return true;
	}
	
	@Override
	public void onKillEntity(EntityLivingBase entity) {

		if (entity instanceof EntityVillager) {

			Entity e = EntityList.createEntityByName(REFERENCE.ENTITY.VAMPIRE_NAME, this.worldObj);
			e.copyLocationAndAnglesFrom(entity);
			VillageVampire v=VillageVampireData.get(entity.worldObj).findNearestVillage(entity);
			if(v!=null){
				v.villagerConvertedByVampire();
			}
			this.worldObj.spawnEntityInWorld(e);
		}
		super.onKillEntity(entity);
	}
	
	@Override
	public void onLivingUpdate() {
		if (!this.worldObj.isRemote) {
			float brightness = this.getBrightness(1.0F);
			boolean canSeeSky = this.worldObj.canBlockSeeTheSky(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY),
					MathHelper.floor_double(this.posZ));
			if (brightness > 0.5F) {
				if (this.worldObj.isDaytime() && canSeeSky) {
					float dmg=sundamage;
					if(this.isPotionActive(ModPotion.sunscreen)){
						dmg=dmg/2;
					}
					this.attackEntityFrom(VampirismMod.sunDamage, dmg);
				}
			}
		}
		super.onLivingUpdate();
	}
	
	/**
	 * Adds standard attacking target tasks for player and villager
	 * @param start Starting priority
	 */
	protected void addAttackingTargetTasks(int start){
		this.targetTasks.addTask(start, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true, false, new IEntitySelector() {

			@Override
			public boolean isEntityApplicable(Entity entity) {
				if (entity instanceof EntityPlayer) {
					return VampirePlayer.get((EntityPlayer) entity).getLevel() <= BALANCE.VAMPIRE_FRIENDLY_LEVEL;
				}
				return false;
			}

		}));
		// Search for villagers
		this.targetTasks.addTask(start+1, new EntityAINearestAttackableTarget(this, EntityVillager.class, 0, true, false, new IEntitySelector() {

			@Override
			public boolean isEntityApplicable(Entity entity) {
				if (entity instanceof EntityVillager) {
					return !VampireMob.get((EntityVillager) entity).isVampire();
				}
				return false;
			}

		}));

	}
	
	@Override
	public boolean attackEntityAsMob(Entity entity){
		boolean flag=super.attackEntityAsMob(entity);
		if(flag&&entity instanceof EntityLivingBase&&this.rand.nextInt(3)==0){
			((EntityLivingBase)entity).addPotionEffect(new PotionEffect(Potion.weakness.id,200));
			((EntityLivingBase)entity).addPotionEffect(new PotionEffect(Potion.moveSlowdown.id,100));
		}
		return flag;
	}

}
