export interface IBCAdress {
    line1: string;
    line2: string;
}

export class BCAdress implements IBCAdress {
    line1: string;
    line2: string;

    constructor() {
        this.line1 = "";
        this.line2 = "";
    }

    copy(address: BCAdress): BCAdress {
        let copyAdress = new BCAdress();
        copyAdress.line1 = address.line1
        copyAdress.line2 = address.line2
        return copyAdress;
    }
}


export interface IBCName {
    line1: string;
    line2: string;
    line3: string;
    line4: string;

}

export class BCName implements IBCName {
    line1: string;
    line2: string;
    line3: string;
    line4: string;

    constructor() {
        this.line1 = "";
        this.line2 = "";
        this.line3 = "";
        this.line4 = "";
    }

    copy(name: BCName): BCName {
        let copyName = new BCName();
        copyName.line1 = name.line1
        copyName.line2 = name.line2
        copyName.line3 = name.line3
        copyName.line4 = name.line4
        return copyName;
    }
}


export interface IBCSignature {
    line1: string;
    line2: string;
}

export class BCSignature implements IBCSignature {
    line1: string;
    line2: string;

    constructor() {
        this.line1 = "";
        this.line2 = "";
    }

    copy(signature: BCSignature): BCSignature {
        let copySignature = new BCSignature();
        copySignature.line1 = signature.line1
        copySignature.line2 = signature.line2
        return copySignature;
    }
}

export interface IBCOptions {
    img: any;
    name: BCName;
    address: BCAdress;
    signature: BCSignature;

    copy(bcOptions: BcOptions): BcOptions;
}

export class BcOptions implements IBCOptions {
    constructor() {
        this.address = new BCAdress();
        this.name = new BCName();
        this.signature = new BCSignature();
        this.img = undefined;
    }


    address: BCAdress;
    img: any;
    name: BCName;
    signature: BCSignature;

    copy(bcOptions: BcOptions): BcOptions {
        let copyBc = new BcOptions();
        copyBc.address = copyBc.address.copy(copyBc.address);
        copyBc.name = copyBc.name.copy(copyBc.name);
        copyBc.signature = copyBc.signature.copy(copyBc.signature);
        return copyBc;
    }
}
