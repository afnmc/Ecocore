package com.azthera.ecocore.gui.impl;

import com.azthera.ecocore.gui.GuiButton;
import com.azthera.ecocore.gui.GuiIconResolver;
import com.azthera.ecocore.gui.GuiSessionManager;
import com.azthera.ecocore.gui.PaginatedGui;
import com.azthera.ecocore.gui.SearchFilter;
import com.azthera.ecocore.gui.SortOption;
import com.azthera.ecocore.shop.ShopCategory;
import com.azthera.ecocore.shop.ShopItemDefinition;
import com.azthera.ecocore.shop.ShopManager;
import com.azthera.ecocore.shop.ShopTransactionService;
import com.azthera.ecocore.util.ItemBuilder;
import com.azthera.ecocore.util.MessageService;
import com.azthera.ecocore.util.NumberFormatter;
import com.azthera.ecocore.util.Result;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;

/**
 * The Dynamic Shop main menu with CATEGORY-FIRST flow.
 * 1. If categoryId is null -> show category buttons.
 * 2. If categoryId is not null -> show items in that category.
 * 
 * Each item displays both buy price (gold) and sell price (green).
 * Left-click to buy, right-click does nothing (sell is in SellGui).
 */
public final class ShopGui extends PaginatedGui {
    private final ShopManager shopManager;
    private final ShopTransactionService shopTransactionService;
    private final GuiIconResolver iconResolver;
    private final MessageService messageService;
    private final NumberFormatter numberFormatter;
    private final YamlConfiguration guiConfig;
    private final String categoryId;
    private final SearchFilter searchFilter = new SearchFilter();
    private SortOption sortOption = SortOption.NAME_ASC;
    private final int searchSlot;
    private final int sortSlot;
    private final int closeSlot;
    private final List<Integer> contentSlots;

    public ShopGui(ShopManager shopManager, ShopTransactionService shopTransactionService,
                   YamlConfiguration guiConfig, GuiIconResolver iconResolver, MessageService messageService,
                   GuiSessionManager sessionManager, NumberFormatter numberFormatter, String categoryId) {
        super(
            iconResolver.resolveRows(guiConfig, 6) * 9,
            Component.text(stripLegacy(iconResolver.resolveTitle(guiConfig, "Dynamic Shop"))),
            sessionManager,
            computeContentSlots(iconResolver.resolveRows(guiConfig, 6)).size(),
            iconResolver.resolveSlot(guiConfig, "previous-page-slot", 45),
            iconResolver.resolveSlot(guiConfig, "next-page-slot", 53)
        );
        this.shopManager = shopManager;
        this.shopTransactionService = shopTransactionService;
        this.guiConfig = guiConfig;
        this.iconResolver = iconResolver;
        this.messageService = messageService;
        this.numberFormatter = numberFormatter;
        this.categoryId = categoryId;
        this.searchSlot = iconResolver.resolveSlot(guiConfig, "search-slot", 49);
        this.sortSlot = iconResolver.resolveSlot(guiConfig, "sort-slot", 50);
        this.closeSlot = iconResolver.resolveSlot(guiConfig, "close-slot", 48);
        this.contentSlots = computeContentSlots(iconResolver.resolveRows(guiConfig, 6));
    }

    private static String stripLegacy(String raw) {
        return raw == null ? "Dynamic Shop" : raw.replaceAll("<[^>]+>", "");
    }

    private static List<Integer> computeContentSlots(int rows) {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row <= rows - 2; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots;
    }

    @Override
    protected void renderStaticElements() {
        ItemStack filler = iconResolver.resolveIcon(guiConfig, "border-filler");
        fillBorder(filler);
        
        // CATEGORY-FIRST: Jika tidak ada kategori, tampilkan kategori di row 0
        if (categoryId == null) {
            int col = 1;
            for (ShopCategory category : shopManager.getAllCategories()) {
                if (col > 7) break;
                int slot = col++;
                setButton(slot, GuiButton.of(buildCategoryIcon(category), (player, clickType) ->
                    new ShopGui(shopManager, shopTransactionService, guiConfig, iconResolver, messageService,
                        getSessionManager(), numberFormatter, category.getId()).open(player)));
            }
        }
        
        setButton(searchSlot, GuiButton.of(iconResolver.resolveIcon(guiConfig, "search"), (player, clickType) ->
            promptSearch(player)));
        setButton(sortSlot, GuiButton.of(buildSortIcon(), (player, clickType) -> {
            sortOption = sortOption.next();
            render();
        }));
        setButton(closeSlot, GuiButton.of(iconResolver.resolveIcon(guiConfig, "close"), (player, clickType) ->
            player.closeInventory()));
    }

    @Override
    protected List<GuiButton> getAllContentButtons() {
        // Jika categoryId null, return empty (kategori sudah di renderStaticElements)
        if (categoryId == null) {
            return List.of();
        }

        // Jika categoryId tidak null, tampilkan item di kategori tersebut
        List<ShopItemDefinition> definitions = new ArrayList<>(shopManager.getDefinitionsForCategory(categoryId));
        
        // FIX: Wrap dalam ArrayList baru agar mutable sebelum di-sort
        definitions = new ArrayList<>(searchFilter.apply(definitions));
        
        definitions.sort((a, b) -> {
            var recordA = shopManager.getRecord(a.getItemId());
            var recordB = shopManager.getRecord(b.getItemId());
            if (recordA.isEmpty() || recordB.isEmpty()) {
                return 0;
            }
            return sortOption.getComparator().compare(recordA.get(), recordB.get());
        });

        List<GuiButton> buttons = new ArrayList<>();
        for (ShopItemDefinition definition : definitions) {
            shopManager.getRecord(definition.getItemId()).ifPresent(record ->
                buttons.add(buildItemButton(definition)));
        }
        return buttons;
    }

    @Override
    protected List<Integer> getContentSlots() {
        return contentSlots;
    }

    private GuiButton buildItemButton(ShopItemDefinition definition) {
        return GuiButton.of(buildItemIcon(definition), (player, clickType) ->
            handleItemClick(player, definition, clickType));
    }

    private void handleItemClick(Player player, ShopItemDefinition definition, ClickType clickType) {
        // BUY-ONLY: Hanya kiri untuk beli
        if (!clickType.isLeftClick()) {
            return;
        }
        int quantity = clickType.isShiftClick() ? 10 : 1;
        Result<Double> result = shopTransactionService.buy(player.getUniqueId(), definition.getItemId(), quantity);
        if (result.isSuccess()) {
            messageService.send(player, "shop.buy-success",
                MessageService.placeholder("amount", String.valueOf(quantity)),
                MessageService.placeholder("item", definition.getDisplayName()),
                MessageService.placeholder("price", numberFormatter.format(result.orElse(0.0))));
        } else {
            result.onFailure(reason -> player.sendMessage(Component.text(reason, NamedTextColor.RED)));
        }
        render();
    }

    private ItemStack buildItemIcon(ShopItemDefinition definition) {
        Material material = Material.matchMaterial(definition.getMaterial());
        if (material == null) {
            material = Material.STONE;
        }
        var recordOpt = shopManager.getRecord(definition.getItemId());
        double buyPrice = recordOpt.map(r -> r.getCurrentPrice()).orElse(definition.getInitialBasePrice());
        double sellPrice = recordOpt.map(r -> r.getSellPrice()).orElse(definition.getInitialBasePrice() * 0.7);
        int stock = recordOpt.map(r -> r.getStock()).orElse(0);
        ItemBuilder builder = ItemBuilder.of(material).name(Component.text(definition.getDisplayName(), NamedTextColor.YELLOW));
        if (definition.getCustomModelData() > 0) {
            builder.customModelData(definition.getCustomModelData());
        }
        // Tampilkan 2 harga
        builder.lore(Component.text("Harga Beli: " + numberFormatter.format(buyPrice), NamedTextColor.GOLD));
        builder.lore(Component.text("Harga Jual: " + numberFormatter.format(sellPrice), NamedTextColor.GREEN));
        builder.lore(Component.text("Stok: " + stock, stock > 0 ? NamedTextColor.AQUA : NamedTextColor.RED));
        builder.lore(Component.empty());
        if (definition.isBuyable()) {
            builder.lore(Component.text("Klik kiri: Beli 1", NamedTextColor.GRAY));
            builder.lore(Component.text("Shift-klik kiri: Beli 10", NamedTextColor.GRAY));
        }
        if (definition.isSellable()) {
            builder.lore(Component.text("Untuk jual, buka /ecocore sell", NamedTextColor.DARK_GRAY));
        }
        return builder.build();
    }

    private ItemStack buildCategoryIcon(ShopCategory category) {
        Material material = Material.matchMaterial(category.getIconMaterial());
        if (material == null) {
            material = Material.CHEST;
        }
        return ItemBuilder.of(material)
            .name(Component.text(category.getDisplayName(), NamedTextColor.AQUA))
            .lore(Component.text("Klik untuk melihat item.", NamedTextColor.GRAY))
            .build();
    }

    private ItemStack buildSortIcon() {
        return ItemBuilder.of(iconResolver.resolveMaterialOnly(guiConfig, "sort", Material.HOPPER))
            .name(Component.text("Sort: " + sortOption.name(), NamedTextColor.LIGHT_PURPLE))
            .build();
    }

    @Override
    protected GuiButton buildPreviousPageButton() {
        return GuiButton.display(iconResolver.resolveIcon(guiConfig, "previous-page"));
    }

    @Override
    protected GuiButton buildNextPageButton() {
        return GuiButton.display(iconResolver.resolveIcon(guiConfig, "next-page"));
    }

    @SuppressWarnings("removal")
    private void promptSearch(Player player) {
        player.closeInventory();
        ConversationFactory factory = new ConversationFactory(JavaPlugin.getProvidingPlugin(ShopGui.class))
            .withFirstPrompt(new StringPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return "Ketik kata kunci pencarian, atau 'batal' untuk membatalkan.";
                }
                @Override
                public Prompt acceptInput(ConversationContext context, String input) {
                    if (!"batal".equalsIgnoreCase(input)) {
                        searchFilter.setQuery(input);
                    }
                    return Prompt.END_OF_CONVERSATION;
                }
            })
            .withLocalEcho(false)
            .addConversationAbandonedListener((ConversationAbandonedEvent event) -> {
                if (event.getContext().getForWhom() instanceof Conversable conversable
                    && conversable instanceof Player conversingPlayer) {
                    render();
                    open(conversingPlayer);
                }
            });
        factory.buildConversation(player).begin();
    }
            }
