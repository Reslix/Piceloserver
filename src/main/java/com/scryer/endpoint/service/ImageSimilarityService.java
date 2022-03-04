package com.scryer.endpoint.service;

/**
 * function knn_search is
 *     input:
 *         t, the target point for the query
 *         k, the number of nearest neighbors of t to search for
 *         Q, max-first priority queue containing at most k points
 *         B, a node, or ball, in the tree
 *     output:
 *         Q, containing the k nearest neighbors from within B
 *
 *     if distance(t, B.pivot) - B.radius ≥ distance(t, Q.first) then
 *         return Q unchanged
 *     else if B is a leaf node then
 *         for each point p in B do
 *             if distance(t, p) < distance(t, Q.first) then
 *                 add p to Q
 *                 if size(Q) > k then
 *                     remove the furthest neighbor from Q
 *                 end if
 *             end if
 *         repeat
 *     else
 *         let child1 be the child node closest to t
 *         let child2 be the child node furthest from t
 *         knn_search(t, k, Q, child1)
 *         knn_search(t, k, Q, child2)
 *     end if
 *     return Q
 * end function[2]
 */
public class ImageSimilarityService {
}
