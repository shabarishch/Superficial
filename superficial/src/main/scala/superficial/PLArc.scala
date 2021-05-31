package superficial
import scala.collection.parallel._, immutable.ParSet

import Polygon.Index
import scala.tools.nsc.doc.html.HtmlTags
import doodle.syntax.path

case class PLArc(
    base: NormalArc[SkewPantsHexagon],
    initialDisplacement: BigDecimal,
    finalDisplacement: BigDecimal
) {
  require(
    !(base.face.boundary(base.initial).isInstanceOf[BoundaryEdge] || base.face
      .boundary(base.terminal)
      .isInstanceOf[BoundaryEdge])
  )
  val hexagonInitialDisplacement: Option[Double] =
    base.face.boundary(base.initial) match {
      case b: BoundaryEdge => None
      case s: SkewCurveEdge =>
        Some(
          SkewPantsHexagon.displacementFromPantsBoundaryVertex(
            base.face,
            s,
            initialDisplacement.doubleValue
          )
        )
      case p: PantsSeam => Some(initialDisplacement.doubleValue)
    }
  val hexagonFinalDisplacement: Option[Double] =
    base.face.boundary(base.terminal) match {
      case b: BoundaryEdge => None
      case s: SkewCurveEdge =>
        Some(
          SkewPantsHexagon.displacementFromPantsBoundaryVertex(
            base.face,
            s,
            finalDisplacement.doubleValue
          )
        )
      case p: PantsSeam => Some(finalDisplacement.doubleValue)
    }
  val length: Double = {
    require(
      base.face.edgeLengths
        .forall(x => x.isDefined) && hexagonInitialDisplacement.isDefined && hexagonFinalDisplacement.isDefined
    )
    if (base.face.top) {
      Hexagon
        .Hyperbolic(
          base.face.edgeLengths(0).get,
          base.face.edgeLengths(1).get,
          base.face.edgeLengths(2).get
        )
        .arcLength(
          SkewPantsHexagon.skewIndexToHexagonIndex(base.face, base.initial),
          SkewPantsHexagon.skewIndexToHexagonIndex(base.face, base.terminal),
          hexagonInitialDisplacement.get,
          hexagonFinalDisplacement.get
        )
    } else {
      Hexagon
        .Hyperbolic(
          base.face.edgeLengths(0).get,
          base.face.edgeLengths(2).get,
          base.face.edgeLengths(1).get
        )
        .arcLength(
          SkewPantsHexagon.skewIndexToHexagonIndex(base.face, base.initial),
          SkewPantsHexagon.skewIndexToHexagonIndex(base.face, base.terminal),
          hexagonInitialDisplacement.get,
          hexagonFinalDisplacement.get
        )
    }
  }

  /**
    * Checks whether the final point of the PLArc is close to any vertex in its terminalEdge
    * Optionally returns true if close to initialvertex of terminaledge and false if close to terminalalvertex of terminaledge
    * Returns None if the final point is not close to any vertex
    *
    * @param threshold how close the final point should be to a vertex
    * @return
    */
  def finalPointClosetoVertex(threshold: BigDecimal): Option[Boolean] = {
    if (finalDisplacement < threshold) Some(true)
    else if (math.abs(
               base.face
                 .sideLength(base.terminalEdge) - finalDisplacement.toDouble
             ) < threshold) Some(false)
    else None
  }
}

object PLArc {
  def freeEnumerate(
      arc: NormalArc[SkewPantsHexagon],
      sep: BigDecimal
  ): ParSet[PLArc] = {
    require(
      !(arc.terminalEdge.isInstanceOf[BoundaryEdge] || arc.initialEdge
        .isInstanceOf[BoundaryEdge])
    )
    arc.initialEdge match {
      case e1: SkewCurveEdge =>
        arc.terminalEdge match {
          case e2: SkewCurveEdge =>
            for (d1: BigDecimal <- BigDecimal(0) to e1.length by sep;
                 d2: BigDecimal <- BigDecimal(0) to e2.length by sep)
              yield PLArc(arc, d1, d2)
          case s2: PantsSeam =>
            for (d1: BigDecimal <- BigDecimal(0) to e1.length by sep;
                 d2: BigDecimal <- BigDecimal(0) to SkewPantsHexagon
                   .getSeamLength(arc.face, s2) by sep)
              yield PLArc(arc, d1, d2)
        }
      case s1: PantsSeam =>
        arc.terminalEdge match {
          case e2: SkewCurveEdge =>
            for {
              d1: BigDecimal <- BigDecimal(0) to SkewPantsHexagon.getSeamLength(
                arc.face,
                s1
              ) by sep
              d2: BigDecimal <- BigDecimal(0) to e2.length by sep
            } yield PLArc(arc, d1, d2)
          case s2: PantsSeam =>
            for {
              d1: BigDecimal <- BigDecimal(0) to SkewPantsHexagon.getSeamLength(
                arc.face,
                s1
              ) by sep
              d2: BigDecimal <- BigDecimal(0) to SkewPantsHexagon.getSeamLength(
                arc.face,
                s2
              ) by sep
            } yield PLArc(arc, d1, d2)
        }
    }
  }.to(ParSet)

  /**
    * Length 1 PL paths corresponding to a normal arc
    *
    * @param arc a given normal arc
    * @param sep separation of the endpoints
    * @return set of length-1 pl-paths
    */
  def freeEnumeratePath(
      arc: NormalArc[SkewPantsHexagon],
      sep: BigDecimal
  ): ParSet[PLPath] = {
    require(
      !(arc.terminalEdge.isInstanceOf[BoundaryEdge] || arc.initialEdge
        .isInstanceOf[BoundaryEdge])
    )
    arc.initialEdge match {
      case e1: SkewCurveEdge =>
        arc.terminalEdge match {
          case e2: SkewCurveEdge =>
            for {
              d1: BigDecimal <- BigDecimal(0) to e1.length by sep
              d2: BigDecimal <- BigDecimal(0) to e2.length by sep
            } yield
              PLPath(
                NormalPath[SkewPantsHexagon](Vector(arc)),
                Vector(d1),
                Vector(d2)
              )
          case s2: PantsSeam =>
            for {
              d1: BigDecimal <- BigDecimal(0) to e1.length by sep
              d2: BigDecimal <- BigDecimal(0) to SkewPantsHexagon.getSeamLength(
                arc.face,
                s2
              ) by sep
            } yield
              PLPath(
                NormalPath[SkewPantsHexagon](Vector(arc)),
                Vector(d1),
                Vector(d2)
              )
        }
      case s1: PantsSeam =>
        arc.terminalEdge match {
          case e2: SkewCurveEdge =>
            for {
              d1: BigDecimal <- BigDecimal(0) to SkewPantsHexagon.getSeamLength(
                arc.face,
                s1
              ) by sep
              d2: BigDecimal <- BigDecimal(0) to e2.length by sep
            } yield
              PLPath(
                NormalPath[SkewPantsHexagon](Vector(arc)),
                Vector(d1),
                Vector(d2)
              )
          case s2: PantsSeam =>
            for {
              d1: BigDecimal <- BigDecimal(0) to SkewPantsHexagon.getSeamLength(
                arc.face,
                s1
              ) by sep
              d2: BigDecimal <- BigDecimal(0) to SkewPantsHexagon.getSeamLength(
                arc.face,
                s2
              ) by sep
            } yield
              PLPath(
                NormalPath[SkewPantsHexagon](Vector(arc)),
                Vector(d1),
                Vector(d2)
              )
        }
    }
  }.to(ParSet)

  def fixedInitialEnumerate(
      arc: NormalArc[SkewPantsHexagon],
      initialDisplacement: BigDecimal,
      sep: BigDecimal
  ): ParSet[PLArc] = {
    require(
      !(arc.terminalEdge.isInstanceOf[BoundaryEdge] || arc.initialEdge
        .isInstanceOf[BoundaryEdge])
    )
    arc.terminalEdge match {
      case e2: SkewCurveEdge =>
        for (d2: BigDecimal <- BigDecimal(0) to e2.length by sep)
          yield PLArc(arc, initialDisplacement, d2)
      case s2: PantsSeam =>
        for {
          d2: BigDecimal <- BigDecimal(0) to SkewPantsHexagon.getSeamLength(
            arc.face,
            s2
          ) by sep
        } yield PLArc(arc, initialDisplacement, d2)
    }
  }.to(ParSet)
}

case class PLPath(
    base: NormalPath[SkewPantsHexagon],
    initialDisplacements: Vector[BigDecimal],
    finalDisplacements: Vector[BigDecimal]
) {
  require(
    (base.edges.size == initialDisplacements.size) && (base.edges.size == finalDisplacements.size)
  )
  require(
    !(base.edges.head.initialEdge
      .isInstanceOf[BoundaryEdge] || base.edges.last.terminalEdge
      .isInstanceOf[BoundaryEdge])
  )
  val plArcs: Vector[PLArc] = for (i <- (0 to (base.edges.size - 1)).toVector)
    yield PLArc(base.edges(i), initialDisplacements(i), finalDisplacements(i))
  require(
    plArcs.zip(plArcs.tail).forall {
      case (arc1, arc2) =>
        arc1.base.terminalEdge match {
          case s1: SkewCurveEdge =>
            arc2.base.initialEdge match {
              case s2: SkewCurveEdge =>
                SkewCurveEdge.comparePoints(
                  s1,
                  arc1.finalDisplacement,
                  s2,
                  arc2.initialDisplacement
                )
              case p2: PantsSeam => false
            }
          case p1: PantsSeam =>
            arc2.base.initialEdge match {
              case s2: SkewCurveEdge => false
              case p2: PantsSeam =>
                PantsSeam.compareSeamPoints(
                  p1,
                  arc1.finalDisplacement,
                  p2,
                  arc2.initialDisplacement,
                  SkewPantsHexagon.getSeamLength(arc1.base.face, p1)
                )
            }
        }
    }
  )
  lazy val length: Double = plArcs.map(arc => arc.length).sum
  def isClosed(tol: Double): Boolean = base.isClosed && {
    (base.initialEdge == base.terminalEdge) match {
      case true =>
        (math.abs(
          (initialDisplacements.head - finalDisplacements.last).toDouble
        ) < tol)
      case false =>
        base.terminalEdge match {
          case s: SkewCurveEdge =>
            (math.abs(
              (initialDisplacements.head - (s.length - finalDisplacements.last)).toDouble
            ) < tol)
          case p: PantsSeam =>
            (math.abs(
              (initialDisplacements.head - (SkewPantsHexagon.getSeamLength(
                base.terminalFace,
                p
              ) - finalDisplacements.last)).toDouble
            ) < tol)
        }
    }
  }
}

object PLPath {

  /**
    * determine initial displacement of the second arc to glue with first arc
    *
    * @param arc1 first normal arc (not pl-arc)
    * @param arc1displacement final displacement of first arc
    * @param arc2 second normal arc (not pl-arc)
    * @return initial displacement of second arc
    */
  def findInitDisplacement(
      arc1: NormalArc[SkewPantsHexagon],
      arc1displacement: BigDecimal,
      arc2: NormalArc[SkewPantsHexagon]
  ): BigDecimal = {
    require(
      (arc1.terminalEdge == arc2.initialEdge) || (arc1.terminalEdge == arc2.initialEdge.flip)
    )
    require(!(arc1.terminalEdge.isInstanceOf[BoundaryEdge]))
    arc2.initialEdge match {
      case e2: SkewCurveEdge =>
        if (arc1.terminalEdge == arc2.initialEdge) arc1displacement
        else (e2.length - arc1displacement)
      case s2: PantsSeam =>
        if (arc1.terminalEdge == arc2.initialEdge) arc1displacement
        else (SkewPantsHexagon.getSeamLength(arc2.face, s2) - arc1displacement)
    }
  }

  /**
    * Append a PLArc to a PLPath
    *
    * @param accum PLPath to which a PLArc has to be appended from an underlying normalpath
    * @param baseEdges Edges of a NormalPath part of which has been enumerated into a PLPath
    * @param numdone Number of edges of the NormalPath that have been enumerated into accum
    * @param sep Separation of endpoints of the appended PLArc
    * @return Set of PLPaths extending accum by one PLArcs with the endpoints of PLPaths separated by sep
    */
  def appendPLArc(
      accum: ParSet[PLPath],
      baseEdges: Vector[NormalArc[SkewPantsHexagon]],
      numdone: Index,
      sep: BigDecimal
  ): ParSet[PLPath] = {
    if (numdone == baseEdges.size) accum
    else {
      baseEdges(numdone).terminalEdge match {
        case e2: SkewCurveEdge =>
          for {
            path <- accum
            d2: BigDecimal <- (BigDecimal(0) to e2.length by sep)
          } yield
            PLPath(
              path.base.:+(baseEdges(numdone)),
              path.initialDisplacements :+ findInitDisplacement(
                baseEdges(numdone - 1),
                path.finalDisplacements.last,
                baseEdges(numdone)
              ),
              path.finalDisplacements :+ d2
            )
        case s2: PantsSeam =>
          for {
            path <- accum
            d2: BigDecimal <- BigDecimal(0) to SkewPantsHexagon.getSeamLength(
              baseEdges(numdone).face,
              s2
            ) by sep
          } yield
            PLPath(
              path.base.:+(baseEdges(numdone)),
              path.initialDisplacements :+ findInitDisplacement(
                baseEdges(numdone - 1),
                path.finalDisplacements.last,
                baseEdges(numdone)
              ),
              path.finalDisplacements :+ d2
            )
      }
    }.to(ParSet)
  }

  /**
    * From a set of PLPaths with the same base, return a set of PLPaths that contains only the shortest path for each choice of start and end point
    * and only if the shortest path has length less than a specified bound
    *
    * @param paths set of paths
    * @param bound length bound
    * @return
    */
  def pickMinimal(paths: ParSet[PLPath], bound: Double): ParSet[PLPath] = {
    paths
      .filter(path => (path.length <= bound))
      .groupBy(p => (p.initialDisplacements.head, p.finalDisplacements.last))
      .map {
        case (_, s) => s.minBy(_.length)
      }
      .to(ParSet)
  }

  @annotation.tailrec
  def enumMinimalRec(
      accum: ParSet[PLPath],
      baseedges: Vector[NormalArc[SkewPantsHexagon]],
      numdone: Index,
      sep: BigDecimal,
      bound: Double
  ): ParSet[PLPath] = {
    if (numdone == baseedges.size) accum
    else
      enumMinimalRec(
        pickMinimal(appendPLArc(accum, baseedges, numdone, sep), bound),
        baseedges,
        numdone + 1,
        sep,
        bound
      )
  }

  /**
    * For a given NormalPath, enumerate minimal PLPaths with endpoints separated by sep and length less than bound
    *
    * @param base NormalPath to be enumerated
    * @param sep separation between endpoints
    * @param bound length bound
    * @return Set of PLPaths
    */
  def enumMinimal(
      base: NormalPath[SkewPantsHexagon],
      sep: BigDecimal,
      bound: Double
  ): ParSet[PLPath] = {
    enumMinimalRec(
      PLArc
        .freeEnumeratePath(base.edges.head, sep)
        .filter(path => (path.length <= bound)),
      base.edges,
      1,
      sep,
      bound
    )
  }

  /**
    * Given a closed NormalPath, optionally return the minimal PLPath corresponding to it if its length is less than a given bound
    *
    * @param base the NormalPath
    * @param sep separation between endpoints while enumerating PLPaths
    * @param bound length bound
    * @return
    */
  def enumMinimalClosed(
      base: NormalPath[SkewPantsHexagon],
      sep: BigDecimal,
      bound: Double
  ): Option[PLPath] = {
    require(base.isClosed, s"$base is not closed")
    enumMinimal(base, sep, bound)
      .groupBy(_.initialDisplacements.head)
      .map {
        case (_, s) =>
          s.minBy(
            p =>
              math.abs(
                p.initialDisplacements.head.toDouble - findInitDisplacement(
                  p.base.edges.last,
                  p.finalDisplacements.last,
                  p.base.edges.head
                ).toDouble
              )
          )
      }
      .filter { p =>
        math.abs(
          p.initialDisplacements.head.toDouble - findInitDisplacement(
            p.base.edges.last,
            p.finalDisplacements.last,
            p.base.edges.head
          ).toDouble
        ) <= (2 * sep)
      }
      .seq
      .minByOption(_.length)
  }

  def enumMinimalClosedFamily(
      paths: Set[NormalPath[SkewPantsHexagon]],
      sep: BigDecimal,
      bound: Double
  ): Map[NormalPath[SkewPantsHexagon], Option[PLPath]] = {
    require(paths.forall(_.isClosed), "All paths are not closed")
    val pathsvec = paths.toVector
    pathsvec.zip(pathsvec.map(p => enumMinimalClosed(p, sep, bound))).toMap
  }

  /**
    * Shorten a PL-path by repeatedly removing removing short arcs and arcs between adjacent skewcurveedges
    *
    * @param complex the surface
    * @param path the path
    * @param sep sep for enumerating length of shortened path
    * @param bound bound for enumerating length of shortened path
    * @return the shortened path
    */
  def shorten(
      complex: TwoComplex[SkewPantsHexagon],
      path: PLPath,
      sep: BigDecimal,
      bound: Double,
      uniqrepuptoflipandcyclicper: Map[NormalPath[SkewPantsHexagon], NormalPath[SkewPantsHexagon]],
      enumdata: Map[NormalPath[SkewPantsHexagon], Option[PLPath]]
  ): Option[PLPath] = {
    require(path.base.isClosed, "Path is not closed")
    val arcsclosetovertex = path.plArcs
      .map(_.finalPointClosetoVertex(1.5 * sep))
      .zipWithIndex
      .filter(_._1.isDefined)
    if (path.base.isVertexLinking) None
    else if (arcsclosetovertex.nonEmpty) {
      val normalpaths = (for { i <- arcsclosetovertex } yield
        NormalPath.otherWayAroundVertex(complex, path.base, i._2, i._1.get))
      if (normalpaths.contains(None)) None
      else {
        val plpaths = for { path <- normalpaths.flatten } yield
          enumdata.get(uniqrepuptoflipandcyclicper.get(path).get).get
        val newminplpath = plpaths.flatten
          .zip(plpaths.flatten.map(_.length))
          .minByOption(_._2)
        if (newminplpath.isDefined) {
          if (newminplpath.get._2 < path.length)
            shorten(complex, newminplpath.get._1, sep, bound, uniqrepuptoflipandcyclicper, enumdata)
          else Some(path)
        } else Some(path)
      }
    } else Some(path)
  }

  //Unsure of the mathematics behind this, pending
  def isotopicNearby(
      complex: TwoComplex[SkewPantsHexagon],
      baseplpath: PLPath,
      paths: Set[PLPath]
  ): Set[PLPath] = {
    val nearbyarcs = NormalPath.pathNeighbouringArcs(complex, baseplpath.base)
    paths.filter(p => p.base.edges.toSet.subsetOf(nearbyarcs))
  }
}
