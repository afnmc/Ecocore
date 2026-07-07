package com.azthera.ecocore.gui;
 
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.ClickType;
 
import java.util.List;
 
/**
 * Extends {@link AbstractGui} with page-based navigation over an arbitrary
 * list of "content" buttons, reserving the bottom row for previous/next page
 * controls. Subclasses supply the full button list via
 * {@link #getAllContentButtons()} and only need to implement that plus
 * whatever static/header slots they want — pagination math is handled here.
 */
public abstract class PaginatedGui extends AbstractGui {
 
    private final int contentSlotsPerPage;
    private final int previousPageSlot;
    private final int nextPageSlot;
    private int currentPage = 0;
 
    protected PaginatedGui(int size, Component title, GuiSessionManager sessionManager,
                            int contentSlotsPerPage, int previousPageSlot, int nextPageSlot) {
        super(size, title, sessionManager);
        this.contentSlotsPerPage = contentSlotsPerPage;
        this.previousPageSlot = previousPageSlot;
        this.nextPageSlot = nextPageSlot;
    }
 
    /**
     * @return the full, unpaginated list of content buttons to display,
     * already filtered/sorted by the subclass as needed. Recomputed on
     * every {@link #render()} call so filters/sorts stay live.
     */
    protected abstract List<GuiButton> getAllContentButtons();
 
    /**
     * @return the list of inventory slot indices, in order, that content
     * buttons should be placed into on a single page (e.g. all non-border
     * slots). Length should be >= contentSlotsPerPage.
     */
    protected abstract List<Integer> getContentSlots();
 
    @Override
    public void render() {
        clearButtons();
        renderStaticElements();
 
        List<GuiButton> allButtons = getAllContentButtons();
        int totalPages = Math.max(1, (int) Math.ceil(allButtons.size() / (double) contentSlotsPerPage));
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));
 
        int startIndex = currentPage * contentSlotsPerPage;
        int endIndex = Math.min(allButtons.size(), startIndex + contentSlotsPerPage);
        List<Integer> slots = getContentSlots();
 
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex < slots.size()) {
                setButton(slots.get(slotIndex), allButtons.get(i));
            }
        }
 
        if (currentPage > 0) {
            setButton(previousPageSlot, buildPreviousPageButton());
        }
        if (currentPage < totalPages - 1) {
            setButton(nextPageSlot, buildNextPageButton());
        }
    }
 
    /**
     * Hook for subclasses to render category headers, search/sort buttons,
     * border fillers, etc, called at the start of every {@link #render()}
     * before content buttons are placed.
     */
    protected void renderStaticElements() {
    }
 
    protected abstract GuiButton buildPreviousPageButton();
 
    protected abstract GuiButton buildNextPageButton();
 
    public void nextPage() {
        currentPage++;
        render();
    }
 
    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            render();
        }
    }
 
    public int getCurrentPage() {
        return currentPage;
    }
 
    @Override
    public void handleClick(org.bukkit.entity.Player player, int slot, ClickType clickType) {
        if (slot == previousPageSlot) {
            previousPage();
            return;
        }
        if (slot == nextPageSlot) {
            nextPage();
            return;
        }
        super.handleClick(player, slot, clickType);
    }
}
