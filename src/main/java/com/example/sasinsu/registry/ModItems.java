package com.example.sasinsu.registry;

import com.example.sasinsu.SasinSuMod;
import com.example.sasinsu.item.DivineWeaponItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SasinSuMod.MODID);

    public static final RegistryObject<Item> AZURE_DRAGON_SWORD = ITEMS.register("azure_dragon_sword",
            () -> new DivineWeaponItem(DivineWeaponItem.BEAST.AZURE_DRAGON,
                    new Item.Properties().stacksTo(1).durability(1800)));
    public static final RegistryObject<Item> WHITE_TIGER_AXE = ITEMS.register("white_tiger_axe",
            () -> new DivineWeaponItem(DivineWeaponItem.BEAST.WHITE_TIGER,
                    new Item.Properties().stacksTo(1).durability(2100)));
    public static final RegistryObject<Item> VERMILION_DAGGER = ITEMS.register("vermilion_dagger",
            () -> new DivineWeaponItem(DivineWeaponItem.BEAST.VERMILION_BIRD,
                    new Item.Properties().stacksTo(1).durability(1400)));
    public static final RegistryObject<Item> BLACK_TORTOISE_SPEAR = ITEMS.register("black_tortoise_spear",
            () -> new DivineWeaponItem(DivineWeaponItem.BEAST.BLACK_TORTOISE,
                    new Item.Properties().stacksTo(1).durability(2000)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.COMBAT
                || event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(AZURE_DRAGON_SWORD);
            event.accept(WHITE_TIGER_AXE);
            event.accept(VERMILION_DAGGER);
            event.accept(BLACK_TORTOISE_SPEAR);
        }
    }
}
