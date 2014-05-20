/*
 * The MIT License
 *
 * Copyright (c) 2014 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.sf.picard.illumina.parser;

import net.sf.samtools.util.CollectionUtil;

import java.util.*;

/** Represents a tile from TileMetricsOut.bin. Stores information on location (lane & tile #, density, number of clusters and the
 * phasing/prephasing values associated with this tile
 *
 * @author jgentry
 */
public class Tile {
    private final int lane, tile;
    private final float density, clusters;

    private final Map<TileTemplateRead, Float> phasingMap;
    private final Map<TileTemplateRead, Float> prePhasingMap;

    /**
     * @param tilePhasingValues Either one or two TilePhasingValues, corresponding to the FIRST and potentially SECOND template reads
     */
    public Tile(final int lane, final int tile, final float density, final float clusters, final TilePhasingValue... tilePhasingValues) {
        this.lane = lane;
        this.tile = tile;
        this.density = density;
        this.clusters = clusters;

        final Collection<TilePhasingValue> phasingValues = ensureSoleTilePhasingValuesPerRead(Arrays.asList(tilePhasingValues));

        final Map<TileTemplateRead, Float> phasingMap = new HashMap<TileTemplateRead, Float>();
        final Map<TileTemplateRead, Float> prePhasingMap = new HashMap<TileTemplateRead, Float>();

        /** For each of the TileReads, assign their phasing & prephasing values to the respective maps, which we will
         * use later to calculate the medians
         */
        for (final TilePhasingValue phasingValue : phasingValues) {
            phasingMap.put(phasingValue.getTileTemplateRead(), phasingValue.getPhasingValue());
            prePhasingMap.put(phasingValue.getTileTemplateRead(), phasingValue.getPrePhasingValue());
        }

        this.phasingMap = Collections.unmodifiableMap(phasingMap);
        this.prePhasingMap = Collections.unmodifiableMap(prePhasingMap);
    }

    /** Returns the number of this tile's parent lane. */
    public int getLaneNumber() {
        return lane;
    }

    /** Returns the number/name of this tile. */
    public int getTileNumber() {
        return tile;
    }

    /** Returns the cluster density of this tile, in units of [cluster/mm^2]. */
    public float getClusterDensity() {
        return density;
    }

    /** Returns the number of on this tile. */
    public float getClusterCount() {
        return clusters;
    }

    public Map<TileTemplateRead, Float> getPhasingMap() {
        return phasingMap;
    }

    public Map<TileTemplateRead, Float> getPrePhasingMap() {
        return prePhasingMap;
    }

    /** For any given TileTemplateRead, we want to make sure that there is only a single TilePhasingValue */
    private static Collection<TilePhasingValue> ensureSoleTilePhasingValuesPerRead(final Collection<TilePhasingValue> tilePhasingValues) {
        final Map<TileTemplateRead, Collection<TilePhasingValue>> partitionedMap = CollectionUtil.partition(tilePhasingValues,
                new CollectionUtil.Partitioner<TilePhasingValue, TileTemplateRead>() {
                    @Override
                    public TileTemplateRead getPartition(final TilePhasingValue phasingValue) {
                        return phasingValue.getTileTemplateRead();
                    }
                });

        final Collection<TilePhasingValue> newTilePhasingValues = new LinkedList<TilePhasingValue>();
        for (final TileTemplateRead read : partitionedMap.keySet()) {
            newTilePhasingValues.add(CollectionUtil.getSoleElement(partitionedMap.get(read)));
        }

        return newTilePhasingValues;
    }
}
