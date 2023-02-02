export interface IBCAdress {
    line1: string;
    line2: string;
}

export class BCAdress implements IBCAdress {
    line1: string;
    line2: string;

    constructor(data?:IBCAdress) {
        if(data){
            this.build(data)
        } else{
            this.line1 = "";
            this.line2 = "";
        }

    }

    copy(address: BCAdress): BCAdress {
        let copyAddress: BCAdress = new BCAdress();
        copyAddress.line1 = address.line1;
        copyAddress.line2 = address.line2;
        return copyAddress;
    }

    private build(data: IBCAdress) {
        this.line1 = data.line1;
        this.line2 = data.line2;
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

    constructor(data?:IBCName) {
        if(data){
            this.build(data)
        } else {
            this.line1 = "";
            this.line2 = "";
            this.line3 = "";
            this.line4 = "";
        }
    }

    copy(name: BCName): BCName {
        let copyName: BCName = new BCName();
        copyName.line1 = name.line1;
        copyName.line2 = name.line2;
        copyName.line3 = name.line3;
        copyName.line4 = name.line4;
        return copyName;
    }

    private build(data: IBCName) {
        this.line1 = data.line1
        this.line2 = data.line2
        this.line3 = data.line3
        this.line4 = data.line4
    }
}


export interface IBCSignature {
    line1: string;
    line2: string;
}

export class BCSignature implements IBCSignature {
    line1: string;
    line2: string;

    constructor(data?:IBCSignature) {
        if(data){
            this.build(data)
        } else{
            this.line1 = "";
            this.line2 = "";
        }
    }

    copy(signature: BCSignature): BCSignature {
        let copySignature: BCSignature = new BCSignature();
        copySignature.line1 = signature.line1;
        copySignature.line2 = signature.line2;
        return copySignature;
    }

    private build(data: IBCSignature) {
        this.line1 = data.line1
        this.line2 = data.line2
    }
}

export interface IBCOptions {
    img: string;
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
        this.img = "";
    }

    build(data: IBCOptions): BcOptions {
        this.address = new BCAdress(<IBCAdress>data.address )
        this.name = new BCName(<IBCName>data.name)
        this.signature = new BCSignature(<IBCSignature>data.signature)
        this.img = data.img;
        return this;
    }
    address: BCAdress;
    img: string;
    name: BCName;
    signature: BCSignature;

    copy(bcOptions: BcOptions): BcOptions {
        let copyBc: BcOptions = new BcOptions();
        copyBc.address = copyBc.address.copy(bcOptions.address);
        copyBc.name = copyBc.name.copy(bcOptions.name);
        copyBc.signature = copyBc.signature.copy(bcOptions.signature);
        copyBc.img = bcOptions.img
        return copyBc;
    }
}
