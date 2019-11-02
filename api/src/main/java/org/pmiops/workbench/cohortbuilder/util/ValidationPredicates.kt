package org.pmiops.workbench.cohortbuilder.util

import com.google.common.collect.ListMultimap
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.function.Predicate
import java.util.stream.Collectors
import org.apache.commons.lang3.math.NumberUtils
import org.pmiops.workbench.model.Attribute
import org.pmiops.workbench.model.Modifier
import org.pmiops.workbench.model.Operator
import org.pmiops.workbench.model.SearchGroupItem

object ValidationPredicates {

    val isEmpty: Predicate<Collection<*>>
        get() = { params -> params.isEmpty() }

    fun betweenOperator(): Predicate<Any> {
        return { t ->
            Operator.BETWEEN.equals(
                    if (t is Attribute)
                        (t as Attribute).getOperator()
                    else
                        (t as Modifier).getOperator())
        }
    }

    fun notBetweenAndNotInOperator(): Predicate<Any> {
        return { t ->
            !Operator.BETWEEN.equals(
                    if (t is Attribute)
                        (t as Attribute).getOperator()
                    else
                        (t as Modifier).getOperator()) && !Operator.IN.equals(
                    if (t is Attribute)
                        (t as Attribute).getOperator()
                    else
                        (t as Modifier).getOperator())
        }
    }

    fun operandsNotTwo(): Predicate<Any> {
        return { t ->
            (if (t is Attribute) (t as Attribute).getOperands() else (t as Modifier).getOperands())
                    .size() !== 2
        }
    }

    fun operandsEmpty(): Predicate<Any> {
        return { t ->
            (if (t is Attribute) (t as Attribute).getOperands() else (t as Modifier).getOperands())
                    .isEmpty()
        }
    }

    fun operandsNotNumbers(): Predicate<Any> {
        return { t ->
            !(if (t is Attribute) (t as Attribute).getOperands() else (t as Modifier).getOperands())
                    .stream().filter({ o -> !NumberUtils.isNumber(o) }).collect(Collectors.toList<T>()).isEmpty()
        }
    }

    fun operandsNotOne(): Predicate<Any> {
        return { t ->
            (if (t is Attribute) (t as Attribute).getOperands() else (t as Modifier).getOperands())
                    .size() !== 1
        }
    }

    fun operatorNull(): Predicate<Any> {
        return { t -> (if (t is Attribute) (t as Attribute).getOperator() else (t as Modifier).getOperator()) == null }
    }

    fun operandsNotDates(): Predicate<Any> {
        return { t ->
            !(if (t is Attribute) (t as Attribute).getOperands() else (t as Modifier).getOperands())
                    .stream()
                    .filter(
                            { date ->
                                try {
                                    SimpleDateFormat("yyyy-MM-dd").parse(date)
                                    return @t instanceof Attribute ? ((Attribute) t).getOperands() : ((Modifier) t).getOperands())
                                    .stream()
                                            .filter false
                                } catch (pe: ParseException) {
                                    return @t instanceof Attribute ? ((Attribute) t).getOperands() : ((Modifier) t).getOperands())
                                    .stream()
                                            .filter true
                                }
                            })
                    .collect(Collectors.toList<T>())
                    .isEmpty()
        }
    }

    fun temporalGroupNull(): Predicate<SearchGroupItem> {
        return { sgi -> sgi.getTemporalGroup() == null }
    }

    fun notZeroAndNotOne(): Predicate<ListMultimap<Int, SearchGroupItem>> {
        return { itemMap -> !itemMap.keySet().containsAll(Arrays.asList(0, 1)) }
    }
}
