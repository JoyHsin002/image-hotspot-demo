package com.joyhsin.hotspot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public record SceneData(long id,String name,String imageUrl,List<Hotspot> draftHotspots,List<Hotspot> publishedHotspots,int draftVersion,int publishedVersion,Instant updatedAt,Instant publishedAt) {
  public SceneData { draftHotspots=draftHotspots==null?new ArrayList<>():new ArrayList<>(draftHotspots); publishedHotspots=publishedHotspots==null?new ArrayList<>():new ArrayList<>(publishedHotspots); }
  public record Hotspot(String id,@DecimalMin("0") @DecimalMax("1") double xRatio,@DecimalMin("0") @DecimalMax("1") double yRatio,@Size(max=80) String label,@Pattern(regexp="^#[0-9a-fA-F]{6}$") String markerColor,@Valid @NotNull Product product) {}
  public record Product(@Size(max=80) String code,@NotBlank @Size(max=120) String name,@Size(max=40) String price,@Size(max=500) String imageUrl,@Size(max=500) String description,@Size(max=1000) String detailUrl) {}
  public record DraftRequest(@NotNull @Size(max=100) List<@Valid Hotspot> hotspots) {}
}
