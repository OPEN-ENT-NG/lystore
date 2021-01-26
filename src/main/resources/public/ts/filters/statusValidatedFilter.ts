// @ts-ignore
import { ng, idiom } from 'entcore';

export const statusV = ng.filter('statusV', () =>
    (value) => {
        return (value === undefined) ?
            "-":
            (value) ?
                'Validé' :
                'Non validé';
    }
);
